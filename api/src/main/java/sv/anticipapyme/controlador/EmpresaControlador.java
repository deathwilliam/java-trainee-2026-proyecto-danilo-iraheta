package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.anticipapyme.dto.EmpresaActualizacion;
import sv.anticipapyme.dto.EmpresaRespuesta;
import sv.anticipapyme.dto.EmpresaSolicitud;
import sv.anticipapyme.servicio.EmpresaServicio;

import java.util.List;

@Tag(name = "Empresas", description = "Registro y gestion de empresas cedentes y pagadoras")
@RestController
@RequestMapping("/api/empresas")
public class EmpresaControlador {

    private final EmpresaServicio empresas;

    public EmpresaControlador(EmpresaServicio empresas) {
        this.empresas = empresas;
    }

    @Operation(summary = "Listar empresas")
    @GetMapping
    public List<EmpresaRespuesta> listar() {
        return empresas.listar();
    }

    @Operation(summary = "Buscar una empresa por id")
    @GetMapping("/{id}")
    public EmpresaRespuesta buscar(@PathVariable Long id) {
        return empresas.buscarPorId(id);
    }

    @Operation(summary = "Registrar una empresa")
    @PostMapping
    public ResponseEntity<EmpresaRespuesta> crear(@Valid @RequestBody EmpresaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresas.crear(solicitud));
    }

    @Operation(summary = "Editar los datos de una empresa")
    @PatchMapping("/{id}")
    public EmpresaRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody EmpresaActualizacion cambios) {
        return empresas.actualizar(id, cambios);
    }

    @Operation(summary = "Desactivar una empresa", description = "204 si se desactivo; 409 si tiene un cupo ACTIVO")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        empresas.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
