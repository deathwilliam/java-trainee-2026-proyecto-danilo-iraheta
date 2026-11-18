package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import sv.anticipapyme.dto.CupoActualizacion;
import sv.anticipapyme.dto.CupoRespuesta;
import sv.anticipapyme.dto.CupoSolicitud;
import sv.anticipapyme.entidad.Usuario;
import sv.anticipapyme.servicio.CupoServicio;
import sv.anticipapyme.servicio.UsuarioServicio;

import java.util.List;

@Tag(name = "Cupos", description = "Aprobacion y gestion de cupos de pagadores")
@RestController
@RequestMapping("/api/cupos")
public class CupoControlador {

    private final CupoServicio cupos;
    private final UsuarioServicio usuarios;

    public CupoControlador(CupoServicio cupos, UsuarioServicio usuarios) {
        this.cupos = cupos;
        this.usuarios = usuarios;
    }

    @Operation(summary = "Listar cupos")
    @GetMapping
    public List<CupoRespuesta> listar() {
        return cupos.listar();
    }

    @Operation(summary = "Buscar un cupo por id")
    @GetMapping("/{id}")
    public CupoRespuesta buscar(@PathVariable Long id) {
        return cupos.buscarPorId(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar un cupo", description = "201 si se aprobo; 400 si la pagadora ya tiene uno ACTIVO (R3)")
    @PostMapping
    public ResponseEntity<CupoRespuesta> aprobar(@Valid @RequestBody CupoSolicitud solicitud,
                                                 @AuthenticationPrincipal Jwt jwt) {
        Usuario aprobador = usuarios.buscarPorUsername(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(cupos.aprobar(solicitud, aprobador));
    }

    @Operation(summary = "Reactivar o renovar un cupo",
            description = "Reactiva un cupo SUSPENDIDO (a ACTIVO, si la pagadora no tiene otro ACTIVO) o renueva su fecha de vencimiento")
    @PatchMapping("/{id}")
    public CupoRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody CupoActualizacion cambios) {
        return cupos.actualizar(id, cambios);
    }

    @Operation(summary = "Suspender un cupo (dar de baja)",
            description = "Pasa el cupo a SUSPENDIDO. 204 si se suspendio; 409 si respalda operaciones VIGENTES")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        cupos.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
