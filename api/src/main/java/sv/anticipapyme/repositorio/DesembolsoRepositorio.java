package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.anticipapyme.entidad.Desembolso;

import java.util.Optional;

public interface DesembolsoRepositorio extends JpaRepository<Desembolso, Long> {

    @Query(value = "select nextval('seq_desembolso')", nativeQuery = true)
    long siguienteCorrelativo();

    Optional<Desembolso> findByOperacionFactorajeId(Long operacionFactorajeId);
}
