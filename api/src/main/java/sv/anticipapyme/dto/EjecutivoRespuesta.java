package sv.anticipapyme.dto;

public record EjecutivoRespuesta(
        Long id,
        String codigoEmpleado,
        String nombres,
        String apellidos,
        String username,
        Boolean activo) {
}
