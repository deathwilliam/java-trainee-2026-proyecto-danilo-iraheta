package sv.anticipapyme.dto;

import sv.anticipapyme.entidad.enums.EstadoCupo;

import java.time.LocalDate;

public record CupoActualizacion(
        EstadoCupo estado,
        LocalDate fechaVencimiento) {
}
