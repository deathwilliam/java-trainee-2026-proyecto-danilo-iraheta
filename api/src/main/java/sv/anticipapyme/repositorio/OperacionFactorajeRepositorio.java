package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.anticipapyme.entidad.OperacionFactoraje;
import sv.anticipapyme.entidad.enums.EstadoOperacion;

import java.util.List;

public interface OperacionFactorajeRepositorio extends JpaRepository<OperacionFactoraje, Long> {

    @Query(value = "select nextval('seq_operacion')", nativeQuery = true)
    long siguienteCorrelativo();

    boolean existsByCupoPagadorIdAndEstado(Long cupoPagadorId, EstadoOperacion estado);

    List<OperacionFactoraje> findByEmpresaCedenteId(Long empresaCedenteId);
}
