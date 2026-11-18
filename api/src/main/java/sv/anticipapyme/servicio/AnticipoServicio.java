package sv.anticipapyme.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.AnticipoRespuesta;
import sv.anticipapyme.entidad.*;
import sv.anticipapyme.entidad.enums.EstadoCupo;
import sv.anticipapyme.entidad.enums.EstadoFactura;
import sv.anticipapyme.entidad.enums.TipoOperacion;
import sv.anticipapyme.excepcion.*;
import sv.anticipapyme.repositorio.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnticipoServicio {

    private static final Logger log = LoggerFactory.getLogger(AnticipoServicio.class);

    private static final BigDecimal DIAS_ANIO_COMERCIAL = BigDecimal.valueOf(360);
    private static final int PLAZO_MINIMO_DIAS = 15;

    private final FacturaRepositorio facturas;
    private final CupoPagadorRepositorio cupos;
    private final OperacionFactorajeRepositorio operaciones;
    private final DetalleOperacionRepositorio detalles;
    private final DesembolsoRepositorio desembolsos;
    private final EjecutivoRepositorio ejecutivos;

    private final BitacoraServicio bitacora;

    public AnticipoServicio(FacturaRepositorio facturas, CupoPagadorRepositorio cupos, OperacionFactorajeRepositorio operaciones, DetalleOperacionRepositorio detalles, DesembolsoRepositorio desembolsos, EjecutivoRepositorio ejecutivos, BitacoraServicio bitacora) {
        this.facturas = facturas;
        this.cupos = cupos;
        this.operaciones = operaciones;
        this.detalles = detalles;
        this.desembolsos = desembolsos;
        this.ejecutivos = ejecutivos;
        this.bitacora = bitacora;
    }

    @Transactional
    public OperacionFactoraje anticipar(Long empresaCedenteId, List<Long> facturaIds, Usuario usuario) {
        try {
            return ejecutarAnticipo(empresaCedenteId, facturaIds, usuario);
        } catch (RuntimeException e) {
            bitacora.registrarFallido(usuario, TipoOperacion.ANTICIPO_FACTURAS, e.getMessage());
            log.warn("Anticipo rechazado: empresa {} facturas {} (usuario {}): {}", empresaCedenteId, facturaIds, usuario.getUsername(), e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public AnticipoRespuesta armarRespuesta(Long operacionId) {
        // esto ya fue creado por anticipar(); si falta algo, ocurrió un error inesperado.
        OperacionFactoraje operacion = operaciones.findById(operacionId)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("No se encontro la operacion %d recien creada", operacionId)));

        List<DetalleOperacion> detallesOperacion = detalles.buscarConFacturaPorOperacion(operacionId);

        Desembolso desembolsoDeOperacion = desembolsos.findByOperacionFactorajeId(operacionId)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("La operacion %d no tiene desembolso", operacionId)));

        List<AnticipoRespuesta.DetalleRespuesta> detallesRespuesta = detallesOperacion
                .stream()
                .map(d -> new AnticipoRespuesta.DetalleRespuesta(
                        d.getFactura().getCodigoGeneracion(),
                        d.getFactura().getMontoTotal(),
                        d.getMontoDescuento()))
                .toList();

        BigDecimal desembolsado = operacion.getMontoNominalTotal().subtract(operacion.getMontoDescuentoTotal());

        return new AnticipoRespuesta(
                operacion.getNumeroOperacion(),
                desembolsoDeOperacion.getNumeroComprobante(),
                operacion.getEstado().name(),
                detallesRespuesta,
                operacion.getMontoNominalTotal(),
                operacion.getMontoDescuentoTotal(),
                desembolsado);
    }

    private OperacionFactoraje ejecutarAnticipo(Long empresaCedenteId, List<Long> facturaIds, Usuario usuario) {

        if (facturaIds == null || facturaIds.isEmpty()) {
            throw new OperacionRechazadaException("Hay que indicar al menos una factura");
        }
        List<Long> ids = facturaIds.stream().distinct().toList();
        if (ids.size() != facturaIds.size()) {
            throw new OperacionRechazadaException("Hay facturas repetidas en la solicitud");
        }

        List<Factura> facturasAAnticipar = facturas.buscarParaAnticiparConEmpresasPorIds(ids);
        if (facturasAAnticipar.size() != ids.size()) {
            List<Long> idsEncontrados = facturasAAnticipar.stream().map(Factura::getId).toList();
            List<Long> faltantes = new ArrayList<>(ids);
            faltantes.removeAll(idsEncontrados);
            throw new OperacionRechazadaException("No existen las facturas con id " + faltantes);
        }

        Ejecutivo ejecutivo = ejecutivos.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ejecutivo del usuario", usuario.getId()));

        Empresa pagadora = facturasAAnticipar.getFirst().getEmpresaPagadora();

        CupoPagador cupo = cupos.findByEmpresaPagadoraIdAndEstado(pagadora.getId(), EstadoCupo.ACTIVO)
                .orElseThrow(() -> new OperacionRechazadaException(
                        String.format("La empresa %s no tiene un cupo ACTIVO", pagadora.getNombreLegal())));

        LocalDate hoy = LocalDate.now();
        if (cupo.getFechaVencimiento().isBefore(hoy)) {
            throw new OperacionRechazadaException(
                    String.format("El cupo %s vencio el %s", cupo.getNumeroCupo(), cupo.getFechaVencimiento()));
        }

        int plazoMaximo = cupo.getCategoriaRiesgo().getPlazoMaximoDias();
        BigDecimal montoNominalTotal = BigDecimal.ZERO;

        for (Factura factura : facturasAAnticipar) {
            if (factura.getEstado() != EstadoFactura.CONFIRMADA) {
                throw new FacturaNoConfirmadaException(
                        factura.getCodigoGeneracion(), factura.getEstado().name());
            }

            if (!factura.getEmpresaCedente().getId().equals(empresaCedenteId)) {
                throw FacturaNoPerteneceException.deOtraCedente(factura.getCodigoGeneracion());
            }
            if (!factura.getEmpresaPagadora().getId().equals(pagadora.getId())) {
                throw FacturaNoPerteneceException.deOtraPagadora(factura.getCodigoGeneracion());
            }

            long dias = ChronoUnit.DAYS.between(hoy, factura.getFechaVencimiento());
            if (dias < PLAZO_MINIMO_DIAS) {
                throw new PlazoInvalidoException(factura.getCodigoGeneracion());
            }
            if (dias > plazoMaximo) {
                throw new PlazoInvalidoException(factura.getCodigoGeneracion(), plazoMaximo);
            }

            montoNominalTotal = montoNominalTotal.add(factura.getMontoTotal());
        }

        Empresa cedente = facturasAAnticipar.getFirst().getEmpresaCedente();
        if (cedente.getCuentaBancaria() == null || cedente.getCuentaBancaria().isBlank()) {
            throw new OperacionRechazadaException(
                    String.format("La empresa %s no tiene cuenta bancaria registrada", cedente.getNombreLegal()));
        }

        if (montoNominalTotal.compareTo(cupo.getMontoDisponible()) > 0) {
            throw new CupoInsuficienteException(montoNominalTotal, cupo.getMontoDisponible());
        }

        OperacionFactoraje operacion = operaciones.save(new OperacionFactoraje(siguienteNumeroOperacion(), cedente, cupo, ejecutivo));

        BigDecimal montoDescuentoTotal = BigDecimal.ZERO;
        for (Factura factura : facturasAAnticipar) {
            BigDecimal descuento = calcularDescuento(factura, operacion.getTasaDescuentoAplicada(), hoy);
            detalles.save(new DetalleOperacion(operacion, factura, descuento));
            montoDescuentoTotal = montoDescuentoTotal.add(descuento);
        }

        operacion.setMontoNominalTotal(montoNominalTotal);
        operacion.setMontoDescuentoTotal(montoDescuentoTotal);

        operaciones.save(operacion);

        for (Factura factura : facturasAAnticipar) {
            factura.setEstado(EstadoFactura.CEDIDA);
        }

        cupo.setMontoDisponible(cupo.getMontoDisponible().subtract(montoNominalTotal));

        cupos.save(cupo);

        desembolsos.save(new Desembolso(siguienteNumeroComprobante(), operacion));

        bitacora.registrarExitoso(usuario, operacion, TipoOperacion.ANTICIPO_FACTURAS,
                String.format("Anticipo de %d factura(s) por $%.2f", facturasAAnticipar.size(), montoNominalTotal));

        log.info("Anticipo realizado: empresa {} facturas {} (usuario {})", empresaCedenteId, facturaIds, usuario.getUsername());

        return operacion;
    }

    private BigDecimal calcularDescuento(Factura factura, BigDecimal tasa, LocalDate hoy) {
        long dias = ChronoUnit.DAYS.between(hoy, factura.getFechaVencimiento());
        return factura.getMontoTotal()
                .multiply(tasa)
                .multiply(BigDecimal.valueOf(dias))
                .divide(DIAS_ANIO_COMERCIAL, 2, RoundingMode.HALF_UP);
    }

    private String siguienteNumeroOperacion() {
        return String.format("OPF-%d-%05d", LocalDate.now().getYear(), operaciones.siguienteCorrelativo());
    }

    private String siguienteNumeroComprobante() {
        return String.format("DES-%d-%05d", LocalDate.now().getYear(), desembolsos.siguienteCorrelativo());
    }
}