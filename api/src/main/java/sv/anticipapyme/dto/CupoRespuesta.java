package sv.anticipapyme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CupoRespuesta(
        Long id,
        String numeroCupo,
        String empresaPagadora,
        String categoriaRiesgo,
        BigDecimal montoAprobado,
        BigDecimal montoDisponible,
        String estado,
        LocalDate fechaAprobacion,
        LocalDate fechaVencimiento) {
}
