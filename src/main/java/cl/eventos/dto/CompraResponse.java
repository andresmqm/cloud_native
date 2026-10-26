package cl.eventos.dto;

import java.math.BigDecimal;

public record CompraResponse(String evento, Integer cantidadEntradas,
                             BigDecimal precioUnitario, BigDecimal precioTotal) {
}
