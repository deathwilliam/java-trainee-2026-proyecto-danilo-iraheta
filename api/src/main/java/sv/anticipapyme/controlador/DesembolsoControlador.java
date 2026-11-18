package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.anticipapyme.dto.DesembolsoRespuesta;
import sv.anticipapyme.servicio.DesembolsoServicio;

import java.util.List;

@Tag(name = "Desembolsos", description = "Consulta de desembolsos a empresas cedentes")
@RestController
@RequestMapping("/api/desembolsos")
public class DesembolsoControlador {

    private final DesembolsoServicio desembolsos;

    public DesembolsoControlador(DesembolsoServicio desembolsos) {
        this.desembolsos = desembolsos;
    }

    @Operation(summary = "Listar desembolsos")
    @GetMapping
    public List<DesembolsoRespuesta> listar() {
        return desembolsos.listar();
    }

    @Operation(summary = "Buscar un desembolso por id")
    @GetMapping("/{id}")
    public DesembolsoRespuesta buscar(@PathVariable Long id) {
        return desembolsos.buscarPorId(id);
    }
}
