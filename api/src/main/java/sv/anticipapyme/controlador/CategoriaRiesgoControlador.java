package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.anticipapyme.dto.CategoriaRiesgoRespuesta;
import sv.anticipapyme.servicio.CategoriaRiesgoServicio;

import java.util.List;

@Tag(name = "Categorias de riesgo", description = "Catalogo de categorias de riesgo de pagadores")
@RestController
@RequestMapping("/api/categorias-riesgo")
public class CategoriaRiesgoControlador {

    private final CategoriaRiesgoServicio categorias;

    public CategoriaRiesgoControlador(CategoriaRiesgoServicio categorias) {
        this.categorias = categorias;
    }

    @Operation(summary = "Listar categorias de riesgo")
    @GetMapping
    public List<CategoriaRiesgoRespuesta> listar() {
        return categorias.listar();
    }

    @Operation(summary = "Buscar una categoria de riesgo por id")
    @GetMapping("/{id}")
    public CategoriaRiesgoRespuesta buscar(@PathVariable Long id) {
        return categorias.buscarPorId(id);
    }
}
