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
import sv.anticipapyme.dto.CobroRespuesta;
import sv.anticipapyme.dto.CobroSolicitud;
import sv.anticipapyme.entidad.Usuario;
import sv.anticipapyme.servicio.CobroServicio;
import sv.anticipapyme.servicio.UsuarioServicio;

import java.util.List;

@Tag(name = "Cobros", description = "Registro y consulta de cobros de facturas")
@RestController
@RequestMapping("/api/cobros")
public class CobroControlador {

    private final CobroServicio cobros;
    private final UsuarioServicio usuarios;

    public CobroControlador(CobroServicio cobros, UsuarioServicio usuarios) {
        this.cobros = cobros;
        this.usuarios = usuarios;
    }

    @Operation(summary = "Listar cobros")
    @GetMapping
    public List<CobroRespuesta> listar() {
        return cobros.listar();
    }

    @Operation(summary = "Buscar un cobro por id")
    @GetMapping("/{id}")
    public CobroRespuesta buscar(@PathVariable Long id) {
        return cobros.buscarPorId(id);
    }

    @PreAuthorize("hasRole('ASESOR')")
    @Operation(summary = "Registrar un cobro",
            description = "201 si se cobro; 400 si la factura no esta CEDIDA/VENCIDA o el monto no cubre capital + mora")
    @PostMapping
    public ResponseEntity<CobroRespuesta> cobrar(@Valid @RequestBody CobroSolicitud solicitud, @AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarios.buscarPorUsername(jwt.getSubject());
        CobroRespuesta respuesta = cobros.cobrar(
                solicitud.facturaId(), solicitud.montoPagado(), solicitud.metodoPago(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}