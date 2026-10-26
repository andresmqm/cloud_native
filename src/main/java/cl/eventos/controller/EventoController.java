package cl.eventos.controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import cl.eventos.model.Evento;
import cl.eventos.repository.EventoRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoRepository eventoRepository;

    public EventoController(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @GetMapping
    public List<Evento> listar() {
        return eventoRepository.findAll();
    }

    @GetMapping("/{id}")
    public Evento buscar(@PathVariable Long id) {   
    return eventoRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
}


   
    @PostMapping
    public Evento agregar(@RequestBody Evento evento) {
        return eventoRepository.save(evento);
    }


    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
    eventoRepository.deleteById(id);
}
}
