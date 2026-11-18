package sv.anticipapyme.dto;

import jakarta.validation.constraints.*;

public record EmpresaSolicitud(
        @NotBlank(message = "El NIT es obligatorio")
        @Pattern(regexp = "\\d{4}-\\d{6}-\\d{3}-\\d",
                message = "El NIT debe tener el formato 0000-000000-000-0")
        String nit,

        @NotBlank(message = "El NRC es obligatorio")
        String nrc,

        @NotBlank(message = "El nombre legal es obligatorio")
        String nombreLegal,

        String nombreComercial,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String correo,

        @NotBlank(message = "El telefono es obligatorio")
        String telefono,

        @NotBlank(message = "La direccion es obligatoria")
        String direccion,

        String cuentaBancaria,

        @NotNull(message = "El sector economico es obligatorio")
        Long sectorEconomicoId) {
}