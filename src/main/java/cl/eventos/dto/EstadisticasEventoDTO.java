package cl.eventos.dto;

import java.math.BigDecimal;

public class EstadisticasEventoDTO {
    private String eventoId;
    private Long totalTickets;
    private Long reservasActivas;
    private BigDecimal recaudacionTotal;

    // Constructor vacío por defecto (requerido para serialización/deserialización)
    public EstadisticasEventoDTO() {
    }

    // Constructor con argumentos
    public EstadisticasEventoDTO(String eventoId, Long totalTickets, Long reservasActivas, BigDecimal recaudacionTotal) {
        this.eventoId = eventoId;
        this.totalTickets = totalTickets;
        this.reservasActivas = reservasActivas;
        this.recaudacionTotal = recaudacionTotal != null ? recaudacionTotal : BigDecimal.ZERO;
    }

    // Getters y Setters
    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public Long getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(Long totalTickets) {
        this.totalTickets = totalTickets;
    }

    public Long getReservasActivas() {
        return reservasActivas;
    }

    public void setReservasActivas(Long reservasActivas) {
        this.reservasActivas = reservasActivas;
    }

    public BigDecimal getRecaudacionTotal() {
        return recaudacionTotal;
    }

    public void setRecaudacionTotal(BigDecimal recaudacionTotal) {
        this.recaudacionTotal = recaudacionTotal;
    }
}