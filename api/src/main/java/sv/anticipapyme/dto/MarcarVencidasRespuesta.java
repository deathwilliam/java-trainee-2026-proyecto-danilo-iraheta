package sv.anticipapyme.dto;

import java.util.List;

public record MarcarVencidasRespuesta(
        List<String> facturasVencidas,
        List<String> operacionesEnMora,
        List<String> cuposVencidos) {
}