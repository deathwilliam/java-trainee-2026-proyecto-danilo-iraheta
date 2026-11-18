package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.anticipapyme.entidad.CupoPagador;
import sv.anticipapyme.entidad.enums.EstadoCupo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CupoPagadorRepositorio extends JpaRepository<CupoPagador, Long> {

    Optional<CupoPagador> findByEmpresaPagadoraIdAndEstado(Long empresaPagadoraId, EstadoCupo estado);

    boolean existsByEmpresaPagadoraIdAndEstado(Long empresaPagadoraId, EstadoCupo estado);

    List<CupoPagador> findByEstadoAndFechaVencimientoBefore(EstadoCupo estado, LocalDate fecha);
}