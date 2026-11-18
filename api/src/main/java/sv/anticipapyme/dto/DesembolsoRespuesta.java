package sv.anticipapyme.dto;

import java.time.LocalDateTime;

public record DesembolsoRespuesta(
        Long id,
        String numeroComprobante,
        String numeroOperacion,
        String cuentaDestino,
        LocalDateTime fechaHora) {
}
