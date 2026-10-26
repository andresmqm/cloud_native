package cl.eventos.controller;

import cl.eventos.dto.CompraRequest;
import cl.eventos.dto.CompraResponse;
import cl.eventos.model.Compra;
import cl.eventos.model.Evento;
import cl.eventos.repository.CompraRepository;
import cl.eventos.repository.EventoRepository;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraRepository compraRepository;
    private final EventoRepository eventoRepository;

    public CompraController(CompraRepository compraRepository, EventoRepository eventoRepository) {
        this.compraRepository = compraRepository;
        this.eventoRepository = eventoRepository;
    }

    @GetMapping
public List<Compra> listarCompras() {
    return compraRepository.findAll();
}


    @PostMapping
    public CompraResponse comprar(@RequestBody CompraRequest request) {
        Evento evento = eventoRepository.findById(request.eventoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

        BigDecimal total = evento.getPrecio().multiply(BigDecimal.valueOf(request.cantidad()));

        Compra compra = new Compra();
        compra.setEvento(evento);
        compra.setCantidad(request.cantidad());
        compra.setPrecioUnitario(evento.getPrecio());
        compra.setPrecioTotal(total);
        compraRepository.save(compra);

        return new CompraResponse(evento.getNombre(), request.cantidad(), evento.getPrecio(), total);
    }
}
