package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.anticipapyme.entidad.Ejecutivo;

import java.util.Optional;

public interface EjecutivoRepositorio extends JpaRepository<Ejecutivo, Long> {

    Optional<Ejecutivo> findByUsuarioId(Long usuarioId);
}