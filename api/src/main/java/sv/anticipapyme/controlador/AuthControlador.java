package sv.anticipapyme.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.anticipapyme.dto.LoginRespuesta;
import sv.anticipapyme.dto.LoginSolicitud;
import sv.anticipapyme.servicio.AuthServicio;

@Tag(name = "Autenticacion", description = "Login y emision de tokens JWT")
@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    private final AuthServicio auth;

    public AuthControlador(AuthServicio auth) {
        this.auth = auth;
    }

    @Operation(summary = "Iniciar sesion", description = "Devuelve un token JWT; 401 si las credenciales son invalidas")
    @PostMapping("/login")
    public LoginRespuesta login(@Valid @RequestBody LoginSolicitud solicitud) {
        return auth.login(solicitud);
    }
}