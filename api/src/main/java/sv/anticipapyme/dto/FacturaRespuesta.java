package sv.anticipapyme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FacturaRespuesta(
        Long id,
        String codigoGeneracion,
        String selloRecepcion,
        BigDecimal montoTotal,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String empresaCedente,
        String empresaPagadora,
        String estado,
        LocalDateTime fechaConfirmacion) {
}
