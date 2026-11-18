package sv.anticipapyme.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CobroRespuesta(
        String numeroComprobante,
        String factura,
        BigDecimal montoCapital,
        BigDecimal montoMora,
        BigDecimal totalCobrado,
        String metodoPago,
        String numeroOperacion,
        String estadoOperacion,
        LocalDateTime fechaHoraPago) {
}