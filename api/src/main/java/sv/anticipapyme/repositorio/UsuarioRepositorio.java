package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.anticipapyme.entidad.Usuario;

import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
}