package sv.anticipapyme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AnticipoSolicitud(
        @NotNull(message = "La empresa cedente es obligatoria")
        Long empresaCedenteId,

        @NotEmpty(message = "Debe incluir al menos una factura")
        List<@NotNull(message = "Los ids de factura no pueden ser nulos") Long>
        facturaIds
) {
}
