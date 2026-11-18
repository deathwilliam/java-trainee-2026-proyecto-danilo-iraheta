package sv.anticipapyme.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sv.anticipapyme.entidad.*;
import sv.anticipapyme.entidad.enums.EstadoCupo;
import sv.anticipapyme.entidad.enums.EstadoFactura;
import sv.anticipapyme.entidad.enums.TipoOperacion;
import sv.anticipapyme.excepcion.*;
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
class AnticipoReglasTest {

    @Mock private FacturaRepositorio facturas;
    @Mock private CupoPagadorRepositorio cupos;
    @Mock private OperacionFactorajeRepositorio operaciones;
    @Mock private DetalleOperacionRepositorio detalles;
    @Mock private DesembolsoRepositorio desembolsos;
    @Mock private EjecutivoRepositorio ejecutivos;
    @Mock private BitacoraServicio bitacora;

    @InjectMocks private AnticipoServicio servicio;

    private final LocalDate hoy = LocalDate.now();

    private SectorEconomico sector;
    private Empresa cedente;
    private Empresa pagadora;
    private Usuario usuario;
    private Ejecutivo ejecutivo;
    private CategoriaRiesgo categoria;
    private CupoPagador cupo;

    @BeforeEach
    void preparar() {
        sector = new SectorEconomico("COM", "Comercio");
        cedente = conId(new Empresa("nit1", "nrc1", "Panaderia La Espiga", "c@c.com", "2222-0000", "SS", sector), 1L);
        cedente.setCuentaBancaria("001234567890");
        pagadora = conId(new Empresa("nit2", "nrc2", "Supermercados del Valle", "p@p.com", "2223-0000", "SS", sector), 2L);
        usuario = conId(new Usuario("asesor1", "hash", "a@a.com"), 10L);
        ejecutivo = conId(new Ejecutivo("EMP-001", "Carla", "Mejia", usuario), 100L);
        categoria = new CategoriaRiesgo("A", "Riesgo bajo", new BigDecimal("0.1800"), new BigDecimal("0.2400"), 120);
        cupo = conId(new CupoPagador("CUP-001", pagadora, categoria, usuario,
                new BigDecimal("50000.00"), hoy, hoy.plusDays(180)), 1L);
    }

    // el servicio compara por getId(); en memoria el id es null, asi que lo seteamos por reflexion
    private static <T> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }

    private Factura factura(Long id, EstadoFactura estado, LocalDate vencimiento, String monto, Empresa ced, Empresa pag) {
        Factura f = new Factura("DTE-" + id, "SELLO-" + id, new BigDecimal(monto), hoy, vencimiento, ced, pag);
        f.setEstado(estado);
        return conId(f, id);
    }

    private Factura facturaBuena(Long id, String monto) {
        return factura(id, EstadoFactura.CONFIRMADA, hoy.plusDays(45), monto, cedente, pagadora);
    }

    @Test
    void casoBuenoMueveTotalesCupoYEstados() {
        Factura f5 = factura(5L, EstadoFactura.CONFIRMADA, hoy.plusDays(45), "4000.00", cedente, pagadora);
        Factura f6 = factura(6L, EstadoFactura.CONFIRMADA, hoy.plusDays(20), "1000.00", cedente, pagadora);
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList())).thenReturn(List.of(f5, f6));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupo));
        when(operaciones.save(any(OperacionFactoraje.class))).thenAnswer(inv -> inv.getArgument(0));

        OperacionFactoraje op = servicio.anticipar(1L, List.of(5L, 6L), usuario);

        assertEquals(new BigDecimal("5000.00"), op.getMontoNominalTotal()); // R11
        assertEquals(new BigDecimal("100.00"), op.getMontoDescuentoTotal()); // R12 (y R10: 90.00 + 10.00)
        assertEquals(new BigDecimal("45000.00"), cupo.getMontoDisponible()); // R13
        assertEquals(EstadoFactura.CEDIDA, f5.getEstado());
        assertEquals(EstadoFactura.CEDIDA, f6.getEstado());
        verify(detalles, times(2)).save(any(DetalleOperacion.class));
        verify(desembolsos).save(any(Desembolso.class));
        verify(cupos).save(cupo);
        verify(bitacora).registrarExitoso(eq(usuario), any(OperacionFactoraje.class), eq(TipoOperacion.ANTICIPO_FACTURAS), anyString());
        verify(bitacora, never()).registrarFallido(any(), any(), any());
    }

    @Test
    void listaVaciaSeRechaza() {
        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.anticipar(1L, List.of(), usuario));

        assertEquals("Hay que indicar al menos una factura", e.getMessage());
        verifyNoInteractions(facturas, cupos, operaciones, detalles, desembolsos, ejecutivos);
        verify(bitacora).registrarFallido(eq(usuario), eq(TipoOperacion.ANTICIPO_FACTURAS), anyString());
    }

    @Test
    void facturasRepetidasSeRechaza() {
        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.anticipar(1L, List.of(5L, 5L), usuario));

        assertEquals("Hay facturas repetidas en la solicitud", e.getMessage());
        verifyNoInteractions(facturas, cupos, operaciones, detalles, desembolsos, ejecutivos);
    }

    @Test
    void facturaInexistenteSeRechaza() {
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList())).thenReturn(List.of());

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.anticipar(1L, List.of(5L), usuario));

        assertEquals("No existen las facturas con id [5]", e.getMessage());
        verify(operaciones, never()).save(any());
    }

    @Test
    void cedenteSinCuentaBancariaSeRechaza() {
        cedente.setCuentaBancaria(null);
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList())).thenReturn(List.of(facturaBuena(5L, "4000.00")));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupo));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.anticipar(1L, List.of(5L), usuario));

        assertEquals("La empresa Panaderia La Espiga no tiene cuenta bancaria registrada", e.getMessage());
        verify(operaciones, never()).save(any());
    }

    @Test
    void r1FacturaNoConfirmadaSeRechaza() {
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList()))
                .thenReturn(List.of(factura(5L, EstadoFactura.REGISTRADA, hoy.plusDays(45), "4000.00", cedente, pagadora)));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupo));

        FacturaNoConfirmadaException e = assertThrows(FacturaNoConfirmadaException.class,
                () -> servicio.anticipar(1L, List.of(5L), usuario));

        assertEquals("La factura DTE-5 tiene estado REGISTRADA, no esta CONFIRMADA", e.getMessage());
        verify(operaciones, never()).save(any());
    }


    @Test
    void r2SinCupoActivoSeRechaza() {
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList())).thenReturn(List.of(facturaBuena(5L, "4000.00")));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.empty());

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.anticipar(1L, List.of(5L), usuario));

        assertEquals("La empresa Supermercados del Valle no tiene un cupo ACTIVO", e.getMessage());
        verify(operaciones, never()).save(any());
    }

    @Test
    void r2CupoVencidoSeRechaza() {
        CupoPagador cupoVencido = conId(new CupoPagador("CUP-OLD", pagadora, categoria, usuario,
                new BigDecimal("50000.00"), hoy.minusDays(200), hoy.minusDays(1)), 2L);
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList())).thenReturn(List.of(facturaBuena(5L, "4000.00")));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupoVencido));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.anticipar(1L, List.of(5L), usuario));

        assertEquals("El cupo CUP-OLD vencio el " + hoy.minusDays(1), e.getMessage());
        verify(operaciones, never()).save(any());
    }

    @Test
    void r7CupoInsuficienteSeRechaza() {
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList())).thenReturn(List.of(facturaBuena(5L, "60000.00")));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupo));

        CupoInsuficienteException e = assertThrows(CupoInsuficienteException.class,
                () -> servicio.anticipar(1L, List.of(5L), usuario));

        assertEquals("El monto de $60000.00 supera el cupo disponible de $50000.00", e.getMessage());
        verify(operaciones, never()).save(any());
    }

    @Test
    void r8FacturaDeOtraCedenteSeRechaza() {
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList())).thenReturn(List.of(facturaBuena(5L, "4000.00")));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupo));

        FacturaNoPerteneceException e = assertThrows(FacturaNoPerteneceException.class,
                () -> servicio.anticipar(99L, List.of(5L), usuario));

        assertEquals("La factura DTE-5 no pertenece a la empresa cedente", e.getMessage());
        verify(operaciones, never()).save(any());
    }

    @Test
    void r8FacturaDeOtraPagadoraSeRechaza() {
        Empresa otraPagadora = conId(new Empresa("nit3", "nrc3", "Otro Super", "o@o.com", "2224-0000", "SS", sector), 3L);
        Factura f5 = facturaBuena(5L, "4000.00");
        Factura f6 = factura(6L, EstadoFactura.CONFIRMADA, hoy.plusDays(30), "1000.00", cedente, otraPagadora);
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList())).thenReturn(List.of(f5, f6));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupo));

        FacturaNoPerteneceException e = assertThrows(FacturaNoPerteneceException.class,
                () -> servicio.anticipar(1L, List.of(5L, 6L), usuario));

        assertEquals("La factura DTE-6 no la debe la pagadora del cupo", e.getMessage());
        verify(operaciones, never()).save(any());
    }

    @Test
    void r9PlazoMuyCortoSeRechaza() {
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList()))
                .thenReturn(List.of(factura(5L, EstadoFactura.CONFIRMADA, hoy.plusDays(10), "4000.00", cedente, pagadora)));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupo));

        PlazoInvalidoException e = assertThrows(PlazoInvalidoException.class,
                () -> servicio.anticipar(1L, List.of(5L), usuario));

        assertEquals("La factura DTE-5 vence en menos de 15 dias", e.getMessage());
        verify(operaciones, never()).save(any());
    }

    @Test
    void r9PlazoMuyLargoSeRechaza() {
        when(facturas.buscarParaAnticiparConEmpresasPorIds(anyList()))
                .thenReturn(List.of(factura(5L, EstadoFactura.CONFIRMADA, hoy.plusDays(200), "4000.00", cedente, pagadora)));
        when(ejecutivos.findByUsuarioId(10L)).thenReturn(Optional.of(ejecutivo));
        when(cupos.findByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(Optional.of(cupo));

        PlazoInvalidoException e = assertThrows(PlazoInvalidoException.class,
                () -> servicio.anticipar(1L, List.of(5L), usuario));

        assertEquals("La factura DTE-5 vence en mas de 120 dias", e.getMessage());
        verify(operaciones, never()).save(any());
    }
}