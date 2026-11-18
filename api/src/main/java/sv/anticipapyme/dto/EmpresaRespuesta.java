package sv.anticipapyme.dto;

public record EmpresaRespuesta(
        Long id,
        String nit,
        String nrc,
        String nombreLegal,
        String nombreComercial,
        String correo,
        String telefono,
        String direccion,
        String cuentaBancaria,
        String sectorEconomico,
        Boolean activo) {
}