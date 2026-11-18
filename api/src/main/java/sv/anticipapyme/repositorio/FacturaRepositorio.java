package sv.anticipapyme.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.anticipapyme.entidad.Factura;
import sv.anticipapyme.entidad.enums.EstadoFactura;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FacturaRepositorio extends JpaRepository<Factura, Long> {

    @Query("select f from Factura f "
            + "join fetch f.empresaCedente "
            + "join fetch f.empresaPagadora "
            + "where f.id in :ids")
    List<Factura> buscarParaAnticiparConEmpresasPorIds(@Param("ids") List<Long> ids);

    List<Factura> findByEstadoAndFechaVencimientoBefore(EstadoFactura estado, LocalDate fecha);

    @Query("select f from Factura f join fetch f.empresaCedente join fetch f.empresaPagadora "
            + "where f.empresaCedente.id = :empresaId or f.empresaPagadora.id = :empresaId")
    List<Factura> buscarDeEmpresa(@Param("empresaId") Long empresaId);
}