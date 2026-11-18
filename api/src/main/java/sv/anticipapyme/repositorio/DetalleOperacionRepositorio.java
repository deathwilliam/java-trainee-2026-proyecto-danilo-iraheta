package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.anticipapyme.entidad.DetalleOperacion;

import java.util.List;
import java.util.Optional;

public interface DetalleOperacionRepositorio extends JpaRepository<DetalleOperacion, Long> {

    @Query("select d from DetalleOperacion d join fetch d.factura where d.operacionFactoraje.id = :operacionId")
    List<DetalleOperacion> buscarConFacturaPorOperacion(Long operacionId);

    Optional<DetalleOperacion> findByFacturaId(Long facturaId);
}
