package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.anticipapyme.entidad.Empresa;

import java.util.Optional;

public interface EmpresaRepositorio extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByUsuarioUsername(String username);
}
