package cl.eventos.service;

import cl.eventos.dto.EstadisticasEventoDTO;
import cl.eventos.dto.TicketRequest;
import cl.eventos.model.Ticket;
import cl.eventos.repository.TicketRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final S3Client s3Client;

    @Value("${efs.path:/app/efs}")
    private String efsBasePath;

    @Value("${aws.s3.bucket:bucket-sesion1}")
    private String bucketName;

    public TicketService(TicketRepository ticketRepository, S3Client s3Client) {
        this.ticketRepository = ticketRepository;
        this.s3Client = s3Client;
    }

    public Ticket generarTicket(TicketRequest request) throws IOException {
        Ticket ticket = new Ticket();
        ticket.setCodigoTicket(request.getCodigoTicket());
        ticket.setEventoId(request.getEventoId());
        ticket.setUsuarioId(request.getUsuarioId());
        ticket.setPrecio(request.getPrecio());

        // 1. Guardar temporalmente en EFS
        File efsDir = new File(efsBasePath);
        if (!efsDir.exists()) {
            efsDir.mkdirs();
        }

        String nombreArchivo = request.getCodigoTicket() + ".pdf";
        File archivoLocal = new File(efsDir, nombreArchivo);

        generarPdfLocal(archivoLocal, ticket);
        ticket.setRutaEfs(archivoLocal.getAbsolutePath());

        // 2. Definir ruta exacta requerida en S3: /{eventoId}/{usuarioId}/{ticketId}.pdf
        String s3Key = request.getEventoId() + "/" + request.getUsuarioId() + "/" + nombreArchivo;
        subirArchivoS3(archivoLocal, s3Key);
        ticket.setS3Key(s3Key);

        return ticketRepository.save(ticket);
    }

    public void subirTicketManualAS3(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + ticketId));

        File archivoLocal = new File(ticket.getRutaEfs());
        if (!archivoLocal.exists()) {
            throw new RuntimeException("El ticket físico no existe en el almacenamiento EFS temporal");
        }
        subirArchivoS3(archivoLocal, ticket.getS3Key());
    }

    private void generarPdfLocal(File destino, Ticket ticket) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(destino));
            document.open();
            document.add(new Paragraph("=========================================="));
            document.add(new Paragraph("           TICKET DE EVENTO               "));
            document.add(new Paragraph("=========================================="));
            document.add(new Paragraph("Código: " + ticket.getCodigoTicket()));
            document.add(new Paragraph("Evento ID: " + ticket.getEventoId()));
            document.add(new Paragraph("Usuario ID: " + ticket.getUsuarioId()));
            document.add(new Paragraph("Precio: $" + ticket.getPrecio()));
            document.add(new Paragraph("Fecha: " + java.time.LocalDateTime.now()));
            document.add(new Paragraph("=========================================="));
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF del ticket", e);
        } finally {
            document.close();
        }
    }

    private void subirArchivoS3(File archivo, String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(archivo));
    }

    public byte[] descargarTicketBytes(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + ticketId));

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(ticket.getS3Key())
                .build();

        ResponseBytes<GetObjectResponse> s3Object = s3Client.getObjectAsBytes(getObjectRequest);
        return s3Object.asByteArray();
    }

    public Ticket modificarTicket(Long id, Ticket detalles) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        if (detalles.getEstado() != null) ticket.setEstado(detalles.getEstado());
        if (detalles.getPrecio() != null) ticket.setPrecio(detalles.getPrecio());
        return ticketRepository.save(ticket);
    }

    public void eliminarTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        // 1. Eliminar de S3
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(ticket.getS3Key())
                    .build());
        } catch (Exception ignored) {}

        // 2. Eliminar de EFS temporal
        try {
            Path path = Paths.get(ticket.getRutaEfs());
            Files.deleteIfExists(path);
        } catch (Exception ignored) {}

        // 3. Eliminar de BD
        ticketRepository.delete(ticket);
    }

    public List<Ticket> buscarPorEvento(String eventoId) {
        return ticketRepository.findByEventoId(eventoId);
    }

    public List<Ticket> buscarPorUsuario(String usuarioId) {
        return ticketRepository.findByUsuarioId(usuarioId);
    }

    public EstadisticasEventoDTO obtenerEstadisticas(String eventoId) {
        return ticketRepository.obtenerEstadisticasPorEvento(eventoId);
    }
}