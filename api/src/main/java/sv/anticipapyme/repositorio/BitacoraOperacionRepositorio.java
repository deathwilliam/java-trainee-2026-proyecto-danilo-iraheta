package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.anticipapyme.entidad.BitacoraOperacion;

public interface BitacoraOperacionRepositorio extends JpaRepository<BitacoraOperacion, Long> {
}
