package sv.anticipapyme.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CupoSolicitud(
        @NotBlank(message = "El numero de cupo es obligatorio")
        String numeroCupo,

        @NotNull(message = "La empresa pagadora es obligatoria")
        Long empresaPagadoraId,

        @NotNull(message = "La categoria de riesgo es obligatoria")
        Long categoriaRiesgoId,

        @NotNull(message = "El monto aprobado es obligatorio")
        @Positive(message = "El monto aprobado debe ser mayor que cero")
        BigDecimal montoAprobado,

        @NotNull(message = "La fecha de aprobacion es obligatoria")
        LocalDate fechaAprobacion,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate fechaVencimiento) {
}
