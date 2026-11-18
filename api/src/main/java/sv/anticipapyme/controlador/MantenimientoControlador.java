package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.anticipapyme.dto.MarcarVencidasRespuesta;
import sv.anticipapyme.servicio.VencimientoServicio;

@Tag(name = "Mantenimiento", description = "Operaciones de mantenimiento del ADMIN")
@RestController
@RequestMapping("/api/mantenimiento")
public class MantenimientoControlador {

    private final VencimientoServicio vencimientos;

    public MantenimientoControlador(VencimientoServicio vencimientos) {
        this.vencimientos = vencimientos;
    }

    @Operation(summary = "Marcar vencidas (R15)",
            description = "Facturas CEDIDAS vencidas pasan a estado VENCIDA (y su operacion EN_MORA). Cupos ACTIVOS vencidos pasan a estado VENCIDO")
    @PostMapping("/marcar-vencidas")
    public MarcarVencidasRespuesta marcarVencidas() {
        return vencimientos.marcarVencidas();
    }
}