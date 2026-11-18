package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.FacturaRespuesta;
import sv.anticipapyme.dto.FacturaSolicitud;
import sv.anticipapyme.entidad.Empresa;
import sv.anticipapyme.entidad.Factura;
import sv.anticipapyme.entidad.enums.EstadoFactura;
import sv.anticipapyme.excepcion.DependenciasActivasException;
import sv.anticipapyme.excepcion.OperacionRechazadaException;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.EmpresaRepositorio;
import sv.anticipapyme.repositorio.FacturaRepositorio;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FacturaServicio {

    private final FacturaRepositorio facturas;
    private final EmpresaRepositorio empresas;

    public FacturaServicio(FacturaRepositorio facturas, EmpresaRepositorio empresas) {
        this.facturas = facturas;
        this.empresas = empresas;
    }

    @Transactional(readOnly = true)
    public List<FacturaRespuesta> listar(String username, boolean verTodo) {
        List<Factura> encontradas = verTodo
                ? facturas.findAll()
                : empresas.findByUsuarioUsername(username)
                .map(e -> facturas.buscarDeEmpresa(e.getId()))
                .orElseGet(List::of);
        return encontradas.stream().map(this::armarRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public FacturaRespuesta buscarPorId(Long id, String username, boolean verTodo) {
        Factura factura = buscarEntidad(id);
        if (!verTodo && !perteneceFactura(factura, username)) {
            throw new RecursoNoEncontradoException("Factura", id);
        }
        return armarRespuesta(factura);
    }

    @Transactional
    public FacturaRespuesta crear(FacturaSolicitud solicitud) {
        if (solicitud.empresaCedenteId().equals(solicitud.empresaPagadoraId())) {
            throw new OperacionRechazadaException("La empresa cedente y la pagadora no pueden ser la misma");
        }
        if (!solicitud.fechaVencimiento().isAfter(solicitud.fechaEmision())) {
            throw new OperacionRechazadaException("La fecha de vencimiento debe ser posterior a la de emision");
        }

        Empresa cedente = buscarEmpresa(solicitud.empresaCedenteId(), "cedente");
        Empresa pagadora = buscarEmpresa(solicitud.empresaPagadoraId(), "pagadora");

        Factura factura = new Factura(
                solicitud.codigoGeneracion(),
                solicitud.selloRecepcion(),
                solicitud.montoTotal(),
                solicitud.fechaEmision(),
                solicitud.fechaVencimiento(),
                cedente,
                pagadora);

        return armarRespuesta(facturas.save(factura));
    }

    @Transactional
    public FacturaRespuesta confirmar(Long id) {
        Factura factura = buscarEntidad(id);

        if (factura.getEstado() != EstadoFactura.REGISTRADA) {
            throw new OperacionRechazadaException(
                    String.format("La factura %s tiene estado %s, solo se confirma una REGISTRADA",
                            factura.getCodigoGeneracion(), factura.getEstado().name()));
        }

        factura.setEstado(EstadoFactura.CONFIRMADA);
        factura.setFechaConfirmacion(LocalDateTime.now());

        return armarRespuesta(facturas.save(factura));
    }

    @Transactional
    public void anular(Long id) {
        Factura factura = buscarEntidad(id);
        if (factura.getEstado() != EstadoFactura.REGISTRADA && factura.getEstado() != EstadoFactura.CONFIRMADA) {
            throw new DependenciasActivasException(
                    String.format("La factura %s tiene estado %s y no se puede anular",
                            factura.getCodigoGeneracion(), factura.getEstado().name()));
        }
        factura.setEstado(EstadoFactura.INVALIDADA);
        facturas.save(factura);
    }

    private Factura buscarEntidad(Long id) {
        return facturas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", id));
    }

    private Empresa buscarEmpresa(Long id, String rol) {
        return empresas.findById(id)
                .orElseThrow(() -> new OperacionRechazadaException(
                        String.format("No existe la empresa %s con id %d", rol, id)));
    }

    private boolean perteneceFactura(Factura factura, String username) {
        return empresas.findByUsuarioUsername(username)
                .map(e -> e.getId().equals(factura.getEmpresaCedente().getId())
                        || e.getId().equals(factura.getEmpresaPagadora().getId()))
                .orElse(false);
    }

    private FacturaRespuesta armarRespuesta(Factura f) {
        return new FacturaRespuesta(
                f.getId(), f.getCodigoGeneracion(), f.getSelloRecepcion(), f.getMontoTotal(),
                f.getFechaEmision(), f.getFechaVencimiento(),
                f.getEmpresaCedente().getNombreLegal(), f.getEmpresaPagadora().getNombreLegal(),
                f.getEstado().name(), f.getFechaConfirmacion());
    }
}
