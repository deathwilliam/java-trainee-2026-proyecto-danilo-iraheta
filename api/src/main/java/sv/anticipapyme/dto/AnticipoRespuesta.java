package sv.anticipapyme.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnticipoRespuesta(
        String numeroOperacion,
        String numeroComprobante,
        String estado,
        List<DetalleRespuesta> facturas,
        BigDecimal montoNominalTotal,
        BigDecimal montoDescuentoTotal,
        BigDecimal montoDesembolsado) {

    public record DetalleRespuesta(
            String codigoGeneracion,
            BigDecimal montoTotal,
            BigDecimal montoDescuento) {
    }
}