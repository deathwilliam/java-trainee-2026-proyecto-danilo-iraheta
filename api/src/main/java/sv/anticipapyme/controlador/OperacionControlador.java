package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.anticipapyme.dto.OperacionRespuesta;
import sv.anticipapyme.servicio.OperacionServicio;

import java.util.List;

@Tag(name = "Operaciones", description = "Consulta de operaciones de factoraje")
@RestController
@RequestMapping("/api/operaciones")
public class OperacionControlador {

    private final OperacionServicio operaciones;

    public OperacionControlador(OperacionServicio operaciones) {
        this.operaciones = operaciones;
    }

    @Operation(summary = "Listar operaciones")
    @GetMapping
    public List<OperacionRespuesta> listar(@AuthenticationPrincipal Jwt jwt) {
        return operaciones.listar(jwt.getSubject(), !esCliente(jwt));
    }

    @Operation(summary = "Buscar una operacion por id")
    @GetMapping("/{id}")
    public OperacionRespuesta buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return operaciones.buscarPorId(id, jwt.getSubject(), !esCliente(jwt));
    }

    private boolean esCliente(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && (roles.contains("CLIENTE"));
    }
}
