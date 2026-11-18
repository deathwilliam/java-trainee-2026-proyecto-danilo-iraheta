package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.anticipapyme.dto.SectorEconomicoRespuesta;
import sv.anticipapyme.servicio.SectorEconomicoServicio;

import java.util.List;

@Tag(name = "Sectores economicos", description = "Catalogo de sectores economicos")
@RestController
@RequestMapping("/api/sectores-economicos")
public class SectorEconomicoControlador {

    private final SectorEconomicoServicio sectores;

    public SectorEconomicoControlador(SectorEconomicoServicio sectores) {
        this.sectores = sectores;
    }

    @Operation(summary = "Listar sectores economicos")
    @GetMapping
    public List<SectorEconomicoRespuesta> listar() {
        return sectores.listar();
    }

    @Operation(summary = "Buscar un sector economico por id")
    @GetMapping("/{id}")
    public SectorEconomicoRespuesta buscar(@PathVariable Long id) {
        return sectores.buscarPorId(id);
    }
}
