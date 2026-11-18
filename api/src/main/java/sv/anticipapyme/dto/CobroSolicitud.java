package sv.anticipapyme.dto;

import jakarta.validation.constraints.*;
import sv.anticipapyme.entidad.enums.MetodoPago;

import java.math.BigDecimal;

public record CobroSolicitud(
        @NotNull(message = "La factura es obligatoria")
        Long facturaId,

        @NotNull(message = "El monto pagado es obligatorio")
        @Positive(message = "El monto pagado debe ser mayor que cero")
        BigDecimal montoPagado,

        @NotNull(message = "El metodo de pago es obligatorio")
        MetodoPago metodoPago
) {
}