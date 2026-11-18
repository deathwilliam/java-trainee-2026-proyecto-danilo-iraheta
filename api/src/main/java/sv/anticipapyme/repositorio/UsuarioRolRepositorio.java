package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.anticipapyme.entidad.UsuarioRol;

import java.util.List;

public interface UsuarioRolRepositorio extends JpaRepository<UsuarioRol, Long> {

    @Query("select ur.rol.nombre from UsuarioRol ur where ur.usuario.id = :usuarioId")
    List<String> buscarNombresRolesPorUsuario(Long usuarioId);
}