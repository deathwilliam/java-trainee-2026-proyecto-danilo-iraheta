package sv.anticipapyme.dto;

import jakarta.validation.constraints.Email;

public record EmpresaActualizacion(
        String nombreComercial,

        @Email(message = "El correo no tiene un formato valido")
        String correo,

        String telefono,

        String direccion,

        String cuentaBancaria,

        Long sectorEconomicoId) {
}