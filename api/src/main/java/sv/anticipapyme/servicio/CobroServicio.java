package sv.anticipapyme.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.CobroRespuesta;
import sv.anticipapyme.entidad.*;
import sv.anticipapyme.entidad.enums.EstadoFactura;
import sv.anticipapyme.entidad.enums.EstadoOperacion;
import sv.anticipapyme.entidad.enums.MetodoPago;
import sv.anticipapyme.entidad.enums.TipoOperacion;
import sv.anticipapyme.excepcion.OperacionRechazadaException;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CobroServicio {

    private static final Logger log = LoggerFactory.getLogger(CobroServicio.class);

    private static final BigDecimal DIAS_ANIO_COMERCIAL = BigDecimal.valueOf(360);

    private final FacturaRepositorio facturas;
    private final CobroRepositorio cobros;
    private final DetalleOperacionRepositorio detalles;
    private final OperacionFactorajeRepositorio operaciones;
    private final EjecutivoRepositorio ejecutivos;
    private final CupoPagadorRepositorio cupos;

    private final BitacoraServicio bitacora;

    public CobroServicio(FacturaRepositorio facturas, CobroRepositorio cobros, DetalleOperacionRepositorio detalles, OperacionFactorajeRepositorio operaciones, EjecutivoRepositorio ejecutivos, CupoPagadorRepositorio cupos, BitacoraServicio bitacora) {
        this.facturas = facturas;
        this.cobros = cobros;
        this.detalles = detalles;
        this.operaciones = operaciones;
        this.ejecutivos = ejecutivos;
        this.cupos = cupos;
        this.bitacora = bitacora;
    }

    @Transactional
    public CobroRespuesta cobrar(Long facturaId, BigDecimal montoPagado, MetodoPago metodoPago, Usuario usuario) {
        try {
            return ejecutarCobro(facturaId, montoPagado, metodoPago, usuario);
        } catch (RuntimeException e) {
            bitacora.registrarFallido(usuario, TipoOperacion.REGISTRO_COBRO, e.getMessage());
            log.warn("Cobro rechazado: factura {} por ${} ({}) (usuario {}): {}",
                    facturaId, montoPagado, metodoPago, usuario.getUsername(), e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<CobroRespuesta> listar() {
        return cobros.findAll().stream().map(this::armarRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public CobroRespuesta buscarPorId(Long id) {
        Cobro cobro = cobros.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cobro", id));
        return armarRespuesta(cobro);
    }

    private CobroRespuesta ejecutarCobro(Long facturaId, BigDecimal montoPagado, MetodoPago metodoPago, Usuario usuario) {
        Ejecutivo ejecutivo = ejecutivos.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ejecutivo del usuario", usuario.getId()));

        Factura factura = facturas.findById(facturaId)
                .orElseThrow(() -> new OperacionRechazadaException(
                        String.format("No existe la factura con id %d", facturaId)));

        if (factura.getEstado() != EstadoFactura.CEDIDA && factura.getEstado() != EstadoFactura.VENCIDA) {
            throw new OperacionRechazadaException(
                    String.format("La factura %s tiene estado %s, solo se cobra una CEDIDA o VENCIDA",
                            factura.getCodigoGeneracion(), factura.getEstado().name()));
        }

        DetalleOperacion detalle = detalles.findByFacturaId(facturaId)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("La factura %s no pertenece a ninguna operacion", factura.getCodigoGeneracion())));
        OperacionFactoraje operacion = detalle.getOperacionFactoraje();

        BigDecimal capital = factura.getMontoTotal();
        long diasAtraso = Math.max(0, ChronoUnit.DAYS.between(factura.getFechaVencimiento(), LocalDate.now()));
        BigDecimal mora = capital
                .multiply(operacion.getTasaMoraAplicada())
                .multiply(BigDecimal.valueOf(diasAtraso))
                .divide(DIAS_ANIO_COMERCIAL, 2, RoundingMode.HALF_UP);

        BigDecimal totalACobrar = exigirMontoExacto(montoPagado, capital, mora);

        Cobro cobro = cobros.save(new Cobro(
                siguienteNumeroComprobante(), factura, ejecutivo, mora, metodoPago, LocalDateTime.now()));

        factura.setEstado(EstadoFactura.COBRADA);
        facturas.save(factura);

        CupoPagador cupo = operacion.getCupoPagador();
        cupo.setMontoDisponible(cupo.getMontoDisponible().add(factura.getMontoTotal()));
        cupos.save(cupo);

        operacion.setEstado(recalcularEstadoOperacion(operacion.getId()));
        operaciones.save(operacion);

        bitacora.registrarExitoso(usuario, operacion, TipoOperacion.REGISTRO_COBRO,
                String.format("Cobro de la factura %s por $%.2f (capital $%.2f + mora $%.2f)",
                        factura.getCodigoGeneracion(), totalACobrar, capital, mora));

        log.info("Cobro realizado: factura {} por ${} ({}) (usuario {})",
                facturaId, montoPagado, metodoPago, usuario.getUsername());

        return armarRespuesta(cobro);
    }

    private BigDecimal exigirMontoExacto(BigDecimal montoPagado, BigDecimal capital, BigDecimal mora) {
        BigDecimal totalACobrar = capital.add(mora);

        int comparacion = montoPagado.compareTo(totalACobrar);
        if (comparacion < 0) {
            throw new OperacionRechazadaException(
                    String.format("El monto de $%.2f no cubre la factura mas su mora ($%.2f)", montoPagado, totalACobrar));
        }
        if (comparacion > 0) {
            throw new OperacionRechazadaException(
                    String.format("El monto de $%.2f excede la factura mas su mora ($%.2f); no se aceptan sobrepagos", montoPagado, totalACobrar));
        }
        return totalACobrar;
    }

    private EstadoOperacion recalcularEstadoOperacion(Long operacionId) {
        List<DetalleOperacion> detallesOp = detalles.buscarConFacturaPorOperacion(operacionId);
        boolean todasCobradas = true;
        boolean algunaVencida = false;
        for (DetalleOperacion d : detallesOp) {
            EstadoFactura estado = d.getFactura().getEstado();
            if (estado != EstadoFactura.COBRADA) todasCobradas = false;
            if (estado == EstadoFactura.VENCIDA) algunaVencida = true;
        }
        if (todasCobradas) return EstadoOperacion.LIQUIDADA;
        if (algunaVencida) return EstadoOperacion.EN_MORA;
        return EstadoOperacion.VIGENTE;
    }

    private String siguienteNumeroComprobante() {
        return String.format("COB-%d-%05d", LocalDate.now().getYear(), cobros.siguienteCorrelativo());
    }

    private CobroRespuesta armarRespuesta(Cobro c) {
        DetalleOperacion detalle = detalles.findByFacturaId(c.getFactura().getId()).orElse(null);
        String numeroOperacion = detalle != null ? detalle.getOperacionFactoraje().getNumeroOperacion() : null;
        String estadoOperacion = detalle != null ? detalle.getOperacionFactoraje().getEstado().name() : null;
        BigDecimal total = c.getMontoCapital().add(c.getMontoMora());

        return new CobroRespuesta(
                c.getNumeroComprobante(), c.getFactura().getCodigoGeneracion(),
                c.getMontoCapital(), c.getMontoMora(), total,
                c.getMetodoPago().name(), numeroOperacion, estadoOperacion, c.getFechaHoraPago());
    }
}