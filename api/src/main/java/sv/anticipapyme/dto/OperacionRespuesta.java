package sv.anticipapyme.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacionRespuesta(
        Long id,
        String numeroOperacion,
        String empresaCedente,
        String numeroCupo,
        String ejecutivo,
        BigDecimal tasaDescuentoAplicada,
        BigDecimal tasaMoraAplicada,
        BigDecimal montoNominalTotal,
        BigDecimal montoDescuentoTotal,
        String estado,
        LocalDateTime fechaHoraOperacion) {
}
