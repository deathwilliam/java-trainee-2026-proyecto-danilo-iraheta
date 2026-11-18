package sv.anticipapyme.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FacturaSolicitud(
        @NotBlank(message = "El codigo de generacion es obligatorio")
        String codigoGeneracion,

        @NotBlank(message = "El sello de recepcion es obligatorio")
        String selloRecepcion,

        @NotNull(message = "El monto total es obligatorio")
        @Positive(message = "El monto total debe ser mayor que cero")
        BigDecimal montoTotal,

        @NotNull(message = "La fecha de emision es obligatoria")
        LocalDate fechaEmision,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate fechaVencimiento,

        @NotNull(message = "La empresa cedente es obligatoria")
        Long empresaCedenteId,

        @NotNull(message = "La empresa pagadora es obligatoria")
        Long empresaPagadoraId) {
}
