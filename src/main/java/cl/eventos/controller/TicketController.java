package cl.eventos.controller;

import cl.eventos.dto.EstadisticasEventoDTO;
import cl.eventos.dto.TicketRequest;
import cl.eventos.model.Ticket;
import cl.eventos.service.TicketService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // 1. Generar tickets (crea PDF en EFS y lo sube a S3)
    @PostMapping("/generar")
    public ResponseEntity<Ticket> generarTicket(@RequestBody TicketRequest request) throws IOException {
        Ticket nuevoTicket = ticketService.generarTicket(request);
        return new ResponseEntity<>(nuevoTicket, HttpStatus.CREATED);
    }

    // 2. Subir tickets a S3 manualmente
    @PostMapping("/{id}/subir-s3")
    public ResponseEntity<String> subirAS3(@PathVariable Long id) {
        ticketService.subirTicketManualAS3(id);
        return ResponseEntity.ok("Ticket subido a S3 correctamente.");
    }

    // 3. Descargar tickets por ID (obtiene el PDF desde S3)
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarTicket(@PathVariable Long id) {
        byte[] pdfData = ticketService.descargarTicketBytes(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    // 4. Consultar tickets por usuario o evento
    @GetMapping
    public ResponseEntity<List<Ticket>> listarTickets(
            @RequestParam(required = false) String eventoId,
            @RequestParam(required = false) String usuarioId) {
        if (eventoId != null) return ResponseEntity.ok(ticketService.buscarPorEvento(eventoId));
        if (usuarioId != null) return ResponseEntity.ok(ticketService.buscarPorUsuario(usuarioId));
        return ResponseEntity.badRequest().build();
    }

    // 5. Modificar detalles del ticket
    @PutMapping("/{id}")
    public ResponseEntity<Ticket> modificarTicket(@PathVariable Long id, @RequestBody Ticket ticket) {
        return ResponseEntity.ok(ticketService.modificarTicket(id, ticket));
    }

    // 6. Eliminar tickets específicos (EFS + S3 + MySQL)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarTicket(@PathVariable Long id) {
        ticketService.eliminarTicket(id);
        return ResponseEntity.ok("Ticket eliminado correctamente de EFS, S3 y Base de Datos.");
    }

    // 7. Reportar estadísticas de ventas y reservas por evento
    @GetMapping("/reportes/eventos/{eventoId}")
    public ResponseEntity<EstadisticasEventoDTO> reportarEstadisticas(@PathVariable String eventoId) {
        return ResponseEntity.ok(ticketService.obtenerEstadisticas(eventoId));
    }
}