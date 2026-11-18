package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.anticipapyme.entidad.Cobro;

public interface CobroRepositorio extends JpaRepository<Cobro, Long> {

    boolean existsByFacturaId(Long facturaId);

    @Query(value = "select nextval('seq_cobro')", nativeQuery = true)
    long siguienteCorrelativo();
}