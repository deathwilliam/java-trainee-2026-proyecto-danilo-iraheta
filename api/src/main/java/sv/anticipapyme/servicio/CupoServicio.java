package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.CupoActualizacion;
import sv.anticipapyme.dto.CupoRespuesta;
import sv.anticipapyme.dto.CupoSolicitud;
import sv.anticipapyme.entidad.CategoriaRiesgo;
import sv.anticipapyme.entidad.CupoPagador;
import sv.anticipapyme.entidad.Empresa;
import sv.anticipapyme.entidad.Usuario;
import sv.anticipapyme.entidad.enums.EstadoCupo;
import sv.anticipapyme.entidad.enums.EstadoOperacion;
import sv.anticipapyme.excepcion.DependenciasActivasException;
import sv.anticipapyme.excepcion.OperacionRechazadaException;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.*;

import java.util.List;

@Service
public class CupoServicio {

    private final CupoPagadorRepositorio cupos;
    private final EmpresaRepositorio empresas;
    private final CategoriaRiesgoRepositorio categorias;
    private final OperacionFactorajeRepositorio operaciones;

    public CupoServicio(CupoPagadorRepositorio cupos, EmpresaRepositorio empresas, CategoriaRiesgoRepositorio categorias, OperacionFactorajeRepositorio operaciones) {
        this.cupos = cupos;
        this.empresas = empresas;
        this.categorias = categorias;
        this.operaciones = operaciones;
    }

    @Transactional(readOnly = true)
    public List<CupoRespuesta> listar() {
        return cupos.findAll()
                .stream()
                .map(this::armarRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CupoRespuesta buscarPorId(Long id) {
        return armarRespuesta(buscarEntidad(id));
    }

    @Transactional
    public CupoRespuesta aprobar(CupoSolicitud solicitud, Usuario aprobador) {
        if (!solicitud.fechaVencimiento().isAfter(solicitud.fechaAprobacion())) {
            throw new OperacionRechazadaException("La fecha de vencimiento debe ser posterior a la de aprobacion");
        }

        Empresa pagadora = empresas.findById(solicitud.empresaPagadoraId())
                .orElseThrow(() -> new OperacionRechazadaException(
                        String.format("No existe la empresa pagadora con id %d", solicitud.empresaPagadoraId())));

        // una pagadora no puede tener dos cupos ACTIVO a la vez
        if (cupos.existsByEmpresaPagadoraIdAndEstado(pagadora.getId(), EstadoCupo.ACTIVO)) {
            throw new OperacionRechazadaException(
                    String.format("La empresa %s ya tiene un cupo ACTIVO", pagadora.getNombreLegal()));
        }

        CategoriaRiesgo categoria = categorias.findById(solicitud.categoriaRiesgoId())
                .orElseThrow(() -> new OperacionRechazadaException(
                        String.format("No existe la categoria de riesgo con id %d", solicitud.categoriaRiesgoId())));

        CupoPagador cupo = new CupoPagador(
                solicitud.numeroCupo(),
                pagadora, categoria,
                aprobador,
                solicitud.montoAprobado(),
                solicitud.fechaAprobacion(),
                solicitud.fechaVencimiento());

        return armarRespuesta(cupos.save(cupo));
    }

    @Transactional
    public CupoRespuesta actualizar(Long id, CupoActualizacion cambios) {
        CupoPagador cupo = buscarEntidad(id);

        if (cambios.estado() != null) {
            if (cambios.estado() == EstadoCupo.SUSPENDIDO) {
                throw new OperacionRechazadaException("Para suspender un cupo usa DELETE; e");
            }
            if (cambios.estado() == EstadoCupo.VENCIDO) {
                throw new OperacionRechazadaException("El estado VENCIDO lo marca el proceso de vencimiento");
            }

            // una pagadora no puede tener dos cupos ACTIVO a la vez
            if (cambios.estado() == EstadoCupo.ACTIVO
                    && cupos.existsByEmpresaPagadoraIdAndEstado(cupo.getEmpresaPagadora().getId(), EstadoCupo.ACTIVO)) {
                throw new OperacionRechazadaException(
                        String.format("La empresa %s ya tiene un cupo ACTIVO", cupo.getEmpresaPagadora().getNombreLegal()));
            }
            cupo.setEstado(cambios.estado());
        }

        if (cambios.fechaVencimiento() != null) cupo.setFechaVencimiento(cambios.fechaVencimiento());

        return armarRespuesta(cupos.save(cupo));
    }

    @Transactional
    public void desactivar(Long id) {
        CupoPagador cupo = buscarEntidad(id);
        if (operaciones.existsByCupoPagadorIdAndEstado(id, EstadoOperacion.VIGENTE)) {
            throw new DependenciasActivasException(
                    String.format("El cupo %s respalda operaciones VIGENTES y no se puede desactivar", cupo.getNumeroCupo()));
        }
        cupo.setEstado(EstadoCupo.SUSPENDIDO);
        cupos.save(cupo);
    }

    private CupoPagador buscarEntidad(Long id) {
        return cupos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cupo pagador", id));
    }

    private CupoRespuesta armarRespuesta(CupoPagador c) {
        return new CupoRespuesta(
                c.getId(), c.getNumeroCupo(), c.getEmpresaPagadora().getNombreLegal(),
                c.getCategoriaRiesgo().getCodigo(), c.getMontoAprobado(), c.getMontoDisponible(),
                c.getEstado().name(), c.getFechaAprobacion(), c.getFechaVencimiento());
    }
}
