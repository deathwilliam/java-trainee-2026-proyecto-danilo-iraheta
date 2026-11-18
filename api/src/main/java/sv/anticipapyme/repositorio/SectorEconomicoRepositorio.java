package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.anticipapyme.entidad.SectorEconomico;

public interface SectorEconomicoRepositorio extends JpaRepository<SectorEconomico, Long> {
}
