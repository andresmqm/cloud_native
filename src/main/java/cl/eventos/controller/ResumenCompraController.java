package cl.eventos.controller;

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

import cl.eventos.service.AwsS3Service;
import io.awspring.cloud.s3.S3Resource;

@RestController
@RequestMapping("/api/resumenes")
public class ResumenCompraController {

    @Autowired
    private AwsS3Service awsS3Service;

    
    //Lista todos los archivos
    
    @GetMapping("/{numeroResumen}/objects")
    public ResponseEntity<List<String>> listObjects(@PathVariable Long numeroResumen) {
        List<String> files = awsS3Service.listarArchivos(numeroResumen);
        return ResponseEntity.ok(files);
    }

    
    //Sube un resumen de compra
    
    @PostMapping(value = "/{numeroResumen}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadObject(@PathVariable Long numeroResumen,
                                      @RequestParam("archivo") MultipartFile file) {
        try {
            String key = awsS3Service.subirArchivo(numeroResumen, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Resumen subido exitosamente",
                    "key", key
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    
     //Descarga un archivo
     
    @GetMapping("/{numeroResumen}/object")
    public ResponseEntity<Resource> downloadObject(@PathVariable Long numeroResumen,
                                                   @RequestParam String key) throws IOException {
        S3Resource recurso = awsS3Service.descargarArchivo(numeroResumen, key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"")
                .body(new InputStreamResource(recurso.getInputStream()));
    }

    
     //Elimina un archivo
    
    @DeleteMapping("/{numeroResumen}/object")
    public ResponseEntity<Void> deleteObject(@PathVariable Long numeroResumen,
                                             @RequestParam String key) {
        awsS3Service.eliminarArchivo(numeroResumen, key);
        return ResponseEntity.noContent().build();
    }


    // Modifica un archivo 

    @PutMapping(value = "/{numeroResumen}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateObject(@PathVariable Long numeroResumen,
                                          @RequestParam("archivo") MultipartFile file) {
        try {
            String key = awsS3Service.modificarArchivo(numeroResumen, file);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Resumen modificado exitosamente",
                    "key", key
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    //eliminar carpeta

    @DeleteMapping("/{numeroResumen}/carpeta")
public ResponseEntity<Void> deleteCarpeta(@PathVariable Long numeroResumen) {
    awsS3Service.eliminarCarpeta(numeroResumen);
    return ResponseEntity.noContent().build();
}
}