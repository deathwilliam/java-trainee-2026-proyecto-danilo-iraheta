package sv.anticipapyme.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sv.anticipapyme.dto.CobroRespuesta;
import sv.anticipapyme.entidad.*;
import sv.anticipapyme.entidad.enums.EstadoFactura;
import sv.anticipapyme.entidad.enums.EstadoOperacion;
import sv.anticipapyme.entidad.enums.MetodoPago;
import sv.anticipapyme.entidad.enums.TipoOperacion;
import sv.anticipapyme.excepcion.OperacionRechazadaException;
import sv.anticipapyme.repositorio.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CobroReglasTest {

    @Mock private FacturaRepositorio facturas;
    @Mock private CobroRepositorio cobros;
    @Mock private DetalleOperacionRepositorio detalles;
    @Mock private OperacionFactorajeRepositorio operaciones;
    @Mock private EjecutivoRepositorio ejecutivos;
    @Mock private CupoPagadorRepositorio cupos;
    @Mock private BitacoraServicio bitacora;

    @InjectMocks private CobroServicio servicio;

    private final LocalDate hoy = LocalDate.now();

    private Empresa cedente;
    private Empresa pagadora;
    private Usuario usuario;
    private Ejecutivo ejecutivo;
    private CupoPagador cupo;
    private OperacionFactoraje operacion;

    @BeforeEach
    void preparar() {
        SectorEconomico sector = new SectorEconomico("COM", "Comercio");
        cedente = conId(new Empresa("nit1", "nrc1", "Panaderia La Espiga", "c@c.com", "2222-0000", "SS", sector), 1L);
        cedente.setCuentaBancaria("001234567890");
        pagadora = conId(new Empresa("nit2", "nrc2", "Supermercados del Valle", "p@p.com", "2223-0000", "SS", sector), 2L);
        usuario = conId(new Usuario("asesor1", "hash", "a@a.com"), 10L);
        ejecutivo = conId(new Ejecutivo("EMP-001", "Carla", "Mejia", usuario), 100L);
        CategoriaRiesgo categoria = new CategoriaRiesgo("A", "Riesgo bajo", new BigDecimal("0.1800"), new BigDecimal("0.2400"), 120);
        cupo = conId(new CupoPagador("CUP-001", pagadora, categoria, usuario, new BigDecimal("50000.00"), hoy, hoy.plusDays(180)), 1L);
        cupo.setMontoDisponible(new BigDecimal("46000.00"));
        operacion = conId(new OperacionFactoraje("OPF-2026-00001", cedente, cupo, ejecutivo), 1L);
    }

    // el servicio compara por getId(); en memoria el id es null, asi que lo seteamos por reflexion
    private static <T> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }

    private Factura factura(Long id, EstadoFactura estado, LocalDate vencimiento, String monto) {
        Factura f = new Factura("DTE-" + id, "SELLO-" + id, new BigDecimal(monto), hoy, vencimiento, cedente, pagadora);
        f.setEstado(estado);
        return conId(f, id);
    }

    private DetalleOperacion detalleDe(Factura f) {
        return new DetalleOperacion(operacion, f, new BigDecimal("0.00"));
    }

    @Test
    void casoBuenoSinMoraLiquidaLaOperacionYLiberaCupo() {
        Factura f = factura(5L, EstadoFactura.CEDIDA, hoy.plusDays(45), "4000.00");
        DetalleOperacion det = detalleDe(f);
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(facturas.findById(5L)).thenReturn(Optional.of(f));
        when(detalles.findByFacturaId(5L)).thenReturn(Optional.of(det));
        when(cobros.save(any(Cobro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalles.buscarConFacturaPorOperacion(1L)).thenReturn(List.of(det));

        CobroRespuesta r = servicio.cobrar(5L, new BigDecimal("4000.00"), MetodoPago.TRANSFERENCIA, usuario);

        assertEquals(EstadoFactura.COBRADA, f.getEstado());
        assertEquals(new BigDecimal("50000.00"), cupo.getMontoDisponible()); // R13 (al cobrar)
        assertEquals(EstadoOperacion.LIQUIDADA, operacion.getEstado()); // R16
        assertEquals(new BigDecimal("0.00"), r.montoMora()); // R14 (sin mora)
        assertEquals(new BigDecimal("4000.00"), r.totalCobrado());
        verify(cobros).save(any(Cobro.class));
        verify(facturas).save(f);
        verify(cupos).save(cupo);
        verify(operaciones).save(operacion);
        verify(bitacora).registrarExitoso(eq(usuario), eq(operacion), eq(TipoOperacion.REGISTRO_COBRO), anyString());
        verify(bitacora, never()).registrarFallido(any(), any(), any());
    }

    @Test
    void facturaInexistenteSeRechaza() {
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(facturas.findById(5L)).thenReturn(Optional.empty());

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.cobrar(5L, new BigDecimal("4000.00"), MetodoPago.TRANSFERENCIA, usuario));

        assertEquals("No existe la factura con id 5", e.getMessage());
        verify(cobros, never()).save(any());
    }

    @Test
    void r14MoraSeCalculaEnFacturaVencida() {
        // 4000 * 0.2400 * 30 / 360 = 80.00
        Factura f = factura(5L, EstadoFactura.VENCIDA, hoy.minusDays(30), "4000.00");
        DetalleOperacion det = detalleDe(f);
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(facturas.findById(5L)).thenReturn(Optional.of(f));
        when(detalles.findByFacturaId(5L)).thenReturn(Optional.of(det));
        when(cobros.save(any(Cobro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalles.buscarConFacturaPorOperacion(1L)).thenReturn(List.of(det));

        CobroRespuesta r = servicio.cobrar(5L, new BigDecimal("4080.00"), MetodoPago.TRANSFERENCIA, usuario);

        assertEquals(new BigDecimal("80.00"), r.montoMora());
        assertEquals(new BigDecimal("4080.00"), r.totalCobrado());
    }

    @Test
    void r14FacturaNoCedidaNiVencidaSeRechaza() {
        Factura f = factura(5L, EstadoFactura.REGISTRADA, hoy.plusDays(45), "4000.00");
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(facturas.findById(5L)).thenReturn(Optional.of(f));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.cobrar(5L, new BigDecimal("4000.00"), MetodoPago.TRANSFERENCIA, usuario));

        assertEquals("La factura DTE-5 tiene estado REGISTRADA, solo se cobra una CEDIDA o VENCIDA", e.getMessage());
        verify(cobros, never()).save(any());
    }

    @Test
    void r14MontoNoCubreSeRechaza() {
        Factura f = factura(5L, EstadoFactura.CEDIDA, hoy.plusDays(45), "4000.00");
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(facturas.findById(5L)).thenReturn(Optional.of(f));
        when(detalles.findByFacturaId(5L)).thenReturn(Optional.of(detalleDe(f)));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.cobrar(5L, new BigDecimal("100.00"), MetodoPago.TRANSFERENCIA, usuario));

        assertEquals("El monto de $100.00 no cubre la factura mas su mora ($4000.00)", e.getMessage());
        verify(cobros, never()).save(any());
    }

    @Test
    void r14SobrepagoSeRechaza() {
        Factura f = factura(5L, EstadoFactura.CEDIDA, hoy.plusDays(45), "4000.00");
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(facturas.findById(5L)).thenReturn(Optional.of(f));
        when(detalles.findByFacturaId(5L)).thenReturn(Optional.of(detalleDe(f)));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.cobrar(5L, new BigDecimal("5000.00"), MetodoPago.TRANSFERENCIA, usuario));

        assertEquals("El monto de $5000.00 excede la factura mas su mora ($4000.00); no se aceptan sobrepagos", e.getMessage());
        verify(cobros, never()).save(any());
    }

    @Test
    void r16OperacionQuedaVigenteSiFaltanFacturasPorCobrar() {
        Factura fa = factura(5L, EstadoFactura.CEDIDA, hoy.plusDays(45), "4000.00");
        Factura fb = factura(6L, EstadoFactura.CEDIDA, hoy.plusDays(30), "1000.00");
        DetalleOperacion detA = detalleDe(fa);
        DetalleOperacion detB = detalleDe(fb);
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(facturas.findById(5L)).thenReturn(Optional.of(fa));
        when(detalles.findByFacturaId(5L)).thenReturn(Optional.of(detA));
        when(cobros.save(any(Cobro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalles.buscarConFacturaPorOperacion(1L)).thenReturn(List.of(detA, detB));

        servicio.cobrar(5L, new BigDecimal("4000.00"), MetodoPago.TRANSFERENCIA, usuario);

        assertEquals(EstadoFactura.COBRADA, fa.getEstado());
        assertEquals(EstadoFactura.CEDIDA, fb.getEstado());
        assertEquals(EstadoOperacion.VIGENTE, operacion.getEstado());
    }

    @Test
    void r16OperacionQuedaEnMoraSiOtraFacturaEstaVencida() {
        Factura fa = factura(5L, EstadoFactura.CEDIDA, hoy.plusDays(45), "4000.00");
        Factura fb = factura(6L, EstadoFactura.VENCIDA, hoy.minusDays(10), "1000.00");
        DetalleOperacion detA = detalleDe(fa);
        DetalleOperacion detB = detalleDe(fb);
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(facturas.findById(5L)).thenReturn(Optional.of(fa));
        when(detalles.findByFacturaId(5L)).thenReturn(Optional.of(detA));
        when(cobros.save(any(Cobro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalles.buscarConFacturaPorOperacion(1L)).thenReturn(List.of(detA, detB));

        servicio.cobrar(5L, new BigDecimal("4000.00"), MetodoPago.TRANSFERENCIA, usuario);

        assertEquals(EstadoFactura.COBRADA, fa.getEstado());
        assertEquals(EstadoOperacion.EN_MORA, operacion.getEstado());   // fb sigue VENCIDA
    }
}