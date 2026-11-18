package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.anticipapyme.dto.EjecutivoRespuesta;
import sv.anticipapyme.servicio.EjecutivoServicio;

import java.util.List;

@Tag(name = "Ejecutivos", description = "Consulta de ejecutivos de banca empresas")
@RestController
@RequestMapping("/api/ejecutivos")
public class EjecutivoControlador {

    private final EjecutivoServicio ejecutivos;

    public EjecutivoControlador(EjecutivoServicio ejecutivos) {
        this.ejecutivos = ejecutivos;
    }

    @Operation(summary = "Listar ejecutivos")
    @GetMapping
    public List<EjecutivoRespuesta> listar() {
        return ejecutivos.listar();
    }

    @Operation(summary = "Buscar un ejecutivo por id")
    @GetMapping("/{id}")
    public EjecutivoRespuesta buscar(@PathVariable Long id) {
        return ejecutivos.buscarPorId(id);
    }
}
