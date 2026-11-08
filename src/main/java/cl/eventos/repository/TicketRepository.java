package cl.eventos.repository;

import cl.eventos.dto.EstadisticasEventoDTO;
import cl.eventos.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByEventoId(String eventoId);

    List<Ticket> findByUsuarioId(String usuarioId);

    Optional<Ticket> findByCodigoTicket(String codigoTicket);

    @Query("SELECT new cl.eventos.dto.EstadisticasEventoDTO(" +
           "t.eventoId, " +
           "COUNT(t), " +
           "SUM(CASE WHEN t.estado = 'RESERVADO' OR t.estado = 'PAGADO' THEN 1L ELSE 0L END), " +
           "COALESCE(SUM(t.precio), 0)) " +
           "FROM Ticket t WHERE t.eventoId = :eventoId GROUP BY t.eventoId")
    EstadisticasEventoDTO obtenerEstadisticasPorEvento(@Param("eventoId") String eventoId);
}