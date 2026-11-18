package sv.anticipapyme.dto;

import java.math.BigDecimal;

public record CategoriaRiesgoRespuesta(
        Long id,
        String codigo,
        String descripcion,
        BigDecimal tasaDescuentoAnual,
        BigDecimal tasaMoraAnual,
        Integer plazoMaximoDias,
        Boolean activo) {
}
