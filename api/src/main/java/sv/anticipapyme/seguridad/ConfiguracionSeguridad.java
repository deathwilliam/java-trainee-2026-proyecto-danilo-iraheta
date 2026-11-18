package sv.anticipapyme.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    private final SecretKey claveSecreta;
    private final HandlerExceptionResolver errores;


    public ConfiguracionSeguridad(@Value("${jwt.clave}") String clave,
                                  @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errores) {
        this.claveSecreta = new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.errores = errores;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveSecreta));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(claveSecreta).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);

        return convertidor;
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/sectores-economicos/**", "/api/categorias-riesgo/**", "/api/facturas/**", "/api/operaciones/**").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/empresas/**", "/api/ejecutivos/**", "/api/cupos/**", "/api/desembolsos/**", "/api/cobros/**").hasAnyRole("ADMIN", "ASESOR")
                        .requestMatchers(HttpMethod.POST, "/api/anticipos").hasRole("ASESOR")
                        .requestMatchers(HttpMethod.POST, "/api/cobros").hasRole("ASESOR")
                        .requestMatchers("/api/cupos/**").hasRole("ADMIN")
                        .requestMatchers("/api/mantenimiento/**").hasRole("ADMIN")
                        .requestMatchers("/api/empresas/**", "/api/facturas/**").hasRole("ASESOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((req, res, e) -> errores.resolveException(req, res, null, e))
                        .accessDeniedHandler((req, res, e) -> errores.resolveException(req, res, null, e)));
        return http.build();
    }
}