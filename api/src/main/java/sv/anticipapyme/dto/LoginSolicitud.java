package sv.anticipapyme.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginSolicitud(
        @NotBlank(message = "El usuario es obligatorio")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        String clave) {
}