package cl.eventos.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import cl.eventos.model.Compra;
import cl.eventos.repository.CompraRepository;
import cl.eventos.service.AwsS3Service;
import cl.eventos.service.EfsService;
import io.awspring.cloud.s3.S3Resource;

@RestController
@RequestMapping("/api/resumenes")
public class ResumenCompraController {

    @Autowired
    private AwsS3Service awsS3Service;

    @Autowired
    private EfsService efsService;

    @Autowired
    private CompraRepository compraRepository;

    // Genera el archivo físico PDF del resumen leyendo la compra desde MySQL para guardarlo en el PC
    @GetMapping("/{numeroResumen}/generar")
    public ResponseEntity<byte[]> generarResumenFisico(@PathVariable Long numeroResumen) {
        Compra compra = compraRepository.findById(numeroResumen)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada con ID: " + numeroResumen));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("=========================================="));
            document.add(new Paragraph("        RESUMEN DE COMPRA #" + compra.getId()));
            document.add(new Paragraph("=========================================="));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Evento: " + compra.getEvento().getNombre()));
            document.add(new Paragraph("Cantidad de entradas: " + compra.getCantidad()));
            document.add(new Paragraph("Precio unitario: $" + compra.getPrecioUnitario()));
            document.add(new Paragraph("Total pagado: $" + compra.getPrecioTotal()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Documento generado para respaldo y carga en S3."));

            document.close();

            byte[] pdfBytes = out.toByteArray();
            String nombreArchivo = "resumen_" + compra.getId() + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Lista todos los archivos
    @GetMapping("/{numeroResumen}/objects")
    public ResponseEntity<List<String>> listObjects(@PathVariable Long numeroResumen) {
        List<String> files = awsS3Service.listarArchivos(numeroResumen);
        return ResponseEntity.ok(files);
    }

    // Sube un resumen de compra (pasando primero por EFS)
    @PostMapping(value = "/{numeroResumen}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadObject(@PathVariable Long numeroResumen,
                                          @RequestParam("archivo") MultipartFile file) {
        try {
            // 1. Guardar temporalmente en EFS
            File archivoEfs = efsService.saveToEfs(file.getOriginalFilename(), file);

            // 2. Subir a S3
            String key = awsS3Service.subirArchivo(numeroResumen, archivoEfs);

            // Si necesitas eliminarlo tras subirlo a S3 (almacenamiento temporal):
            // efsService.deleteFromEfs(archivoTemporal);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Resumen guardado en EFS y subido exitosamente a S3",
                    "key", key
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Descarga un archivo
    @GetMapping("/{numeroResumen}/object")
    public ResponseEntity<Resource> downloadObject(@PathVariable Long numeroResumen,
                                                   @RequestParam String key) throws IOException {
        S3Resource recurso = awsS3Service.descargarArchivo(numeroResumen, key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"")
                .body(new InputStreamResource(recurso.getInputStream()));
    }

    // Elimina un archivo
    @DeleteMapping("/{numeroResumen}/object")
    public ResponseEntity<Void> deleteObject(@PathVariable Long numeroResumen,
                                             @RequestParam String key) {
        awsS3Service.eliminarArchivo(numeroResumen, key);
        return ResponseEntity.noContent().build();
    }

    // Modifica un archivo (pasando primero por EFS)
    @PutMapping(value = "/{numeroResumen}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateObject(@PathVariable Long numeroResumen,
                                          @RequestParam("archivo") MultipartFile file) {
        try {
            // 1. Guardar en EFS usando el nombre real del archivo
            File archivoEfs = efsService.saveToEfs(file.getOriginalFilename(), file);

            // 2. Actualizar en S3
            String key = awsS3Service.modificarArchivo(numeroResumen, archivoEfs);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Resumen modificado temporalmente en EFS y actualizado en S3",
                    "key", key
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Eliminar carpeta
    @DeleteMapping("/{numeroResumen}/carpeta")
    public ResponseEntity<Void> deleteCarpeta(@PathVariable Long numeroResumen) {
        awsS3Service.eliminarCarpeta(numeroResumen);
        return ResponseEntity.noContent().build();
    }
}