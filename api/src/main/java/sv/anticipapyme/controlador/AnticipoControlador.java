package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.anticipapyme.dto.AnticipoRespuesta;
import sv.anticipapyme.dto.AnticipoSolicitud;
import sv.anticipapyme.entidad.OperacionFactoraje;
import sv.anticipapyme.entidad.Usuario;
import sv.anticipapyme.servicio.AnticipoServicio;
import sv.anticipapyme.servicio.UsuarioServicio;

@Tag(name = "Anticipos", description = "La operacion principal: anticipar facturas de una PYME")
@RestController
@RequestMapping("/api/anticipos")
public class AnticipoControlador {

    private final AnticipoServicio anticipos;
    private final UsuarioServicio usuarios;

    public AnticipoControlador(AnticipoServicio anticipos, UsuarioServicio usuarios) {
        this.anticipos = anticipos;
        this.usuarios = usuarios;
    }

    @PreAuthorize("hasRole('ASESOR')")
    @Operation(summary = "Anticipar facturas",
            description = "201 si se hizo; 400 si una regla falla o faltan datos")
    @PostMapping
    public ResponseEntity<AnticipoRespuesta> anticipar(@Valid @RequestBody AnticipoSolicitud solicitud,
                                                       @AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarios.buscarPorUsername(jwt.getSubject());

        OperacionFactoraje operacion = anticipos.anticipar(solicitud.empresaCedenteId(), solicitud.facturaIds(), usuario);
        AnticipoRespuesta respuesta = anticipos.armarRespuesta(operacion.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}