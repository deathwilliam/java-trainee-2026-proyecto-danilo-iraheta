package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import sv.anticipapyme.dto.FacturaRespuesta;
import sv.anticipapyme.dto.FacturaSolicitud;
import sv.anticipapyme.servicio.FacturaServicio;

import java.util.List;

@Tag(name = "Facturas", description = "Registro, confirmacion y consulta de facturas electronicas")
@RestController
@RequestMapping("/api/facturas")
public class FacturaControlador {

    private final FacturaServicio facturas;

    public FacturaControlador(FacturaServicio facturas) {
        this.facturas = facturas;
    }

    @Operation(summary = "Listar facturas")
    @GetMapping
    public List<FacturaRespuesta> listar(@AuthenticationPrincipal Jwt jwt) {
        return facturas.listar(jwt.getSubject(), !esCliente(jwt));
    }

    @Operation(summary = "Buscar una factura por id")
    @GetMapping("/{id}")
    public FacturaRespuesta buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return facturas.buscarPorId(id, jwt.getSubject(), !esCliente(jwt));
    }

    @Operation(summary = "Registrar una factura")
    @PostMapping
    public ResponseEntity<FacturaRespuesta> crear(@Valid @RequestBody FacturaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturas.crear(solicitud));
    }

    @Operation(summary = "Confirmar una factura", description = "Pasa la factura de REGISTRADA a CONFIRMADA")
    @PatchMapping("/{id}/confirmacion")
    public FacturaRespuesta confirmar(@PathVariable Long id) {
        return facturas.confirmar(id);
    }

    @Operation(summary = "Anular (invalidar) una factura", description = "204 si se anulo; 409 si ya fue cedida o cobrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturas.anular(id);
        return ResponseEntity.noContent().build();
    }

    private boolean esCliente(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && (roles.contains("CLIENTE"));
    }
}
