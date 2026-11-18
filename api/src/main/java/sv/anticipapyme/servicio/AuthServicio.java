package sv.anticipapyme.servicio;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.LoginRespuesta;
import sv.anticipapyme.dto.LoginSolicitud;
import sv.anticipapyme.entidad.Usuario;
import sv.anticipapyme.excepcion.CredencialesInvalidasException;
import sv.anticipapyme.repositorio.UsuarioRepositorio;
import sv.anticipapyme.repositorio.UsuarioRolRepositorio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthServicio {

    private final UsuarioRepositorio usuarios;
    private final UsuarioRolRepositorio usuarioRoles;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthServicio(UsuarioRepositorio usuarios, UsuarioRolRepositorio usuarioRoles, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.usuarios = usuarios;
        this.usuarioRoles = usuarioRoles;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional(readOnly = true)
    public LoginRespuesta login(LoginSolicitud solicitud) {

        Usuario usuario = usuarios.findByUsername(solicitud.username())
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(solicitud.clave(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        List<String> roles = usuarioRoles.buscarNombresRolesPorUsuario(usuario.getId());
        return new LoginRespuesta(generarToken(usuario, roles), "Bearer");
    }

    private String generarToken(Usuario usuario, List<String> roles) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("anticipapyme")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(2, ChronoUnit.HOURS))
                .subject(usuario.getUsername())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}