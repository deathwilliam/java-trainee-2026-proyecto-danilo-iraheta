package sv.anticipapyme.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sv.anticipapyme.dto.MarcarVencidasRespuesta;
import sv.anticipapyme.entidad.*;
import sv.anticipapyme.entidad.enums.EstadoCupo;
import sv.anticipapyme.entidad.enums.EstadoFactura;
import sv.anticipapyme.entidad.enums.EstadoOperacion;
import sv.anticipapyme.repositorio.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VencimientoReglasTest {

    @Mock private FacturaRepositorio facturas;
    @Mock private CupoPagadorRepositorio cupos;
    @Mock private DetalleOperacionRepositorio detalles;
    @Mock private OperacionFactorajeRepositorio operaciones;

    @InjectMocks private VencimientoServicio servicio;

    private final LocalDate hoy = LocalDate.now();

    private Empresa cedente;
    private Empresa pagadora;
    private CupoPagador cupo;
    private OperacionFactoraje operacion;

    @BeforeEach
    void preparar() {
        SectorEconomico sector = new SectorEconomico("COM", "Comercio");
        cedente = conId(new Empresa("nit1", "nrc1", "Panaderia La Espiga", "c@c.com", "2222-0000", "SS", sector), 1L);
        pagadora = conId(new Empresa("nit2", "nrc2", "Supermercados del Valle", "p@p.com", "2223-0000", "SS", sector), 2L);
        Usuario usuario = conId(new Usuario("asesor1", "hash", "a@a.com"), 10L);
        Ejecutivo ejecutivo = conId(new Ejecutivo("EMP-001", "Carla", "Mejia", usuario), 100L);
        CategoriaRiesgo categoria = new CategoriaRiesgo("A", "Riesgo bajo", new BigDecimal("0.1800"), new BigDecimal("0.2400"), 120);
        cupo = conId(new CupoPagador("CUP-001", pagadora, categoria, usuario, new BigDecimal("50000.00"), hoy.minusDays(200), hoy.minusDays(1)), 1L);
        operacion = conId(new OperacionFactoraje("OPF-2026-00001", cedente, cupo, ejecutivo), 1L);
    }

    private static <T> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }

    private Factura factura(Long id, EstadoFactura estado, LocalDate vencimiento, String monto) {
        Factura f = new Factura("DTE-" + id, "SELLO-" + id, new BigDecimal(monto), hoy.minusDays(90), vencimiento, cedente, pagadora);
        f.setEstado(estado);
        return conId(f, id);
    }

    @Test
    void sinVencidasNoTocaNada() {
        when(facturas.findByEstadoAndFechaVencimientoBefore(eq(EstadoFactura.CEDIDA), any(LocalDate.class))).thenReturn(List.of());
        when(cupos.findByEstadoAndFechaVencimientoBefore(eq(EstadoCupo.ACTIVO), any(LocalDate.class))).thenReturn(List.of());

        MarcarVencidasRespuesta r = servicio.marcarVencidas();

        assertTrue(r.facturasVencidas().isEmpty());
        assertTrue(r.cuposVencidos().isEmpty());
        verify(facturas, never()).save(any());
        verify(cupos, never()).save(any());
        verify(operaciones, never()).save(any());
    }

    @Test
    void r15FacturaCedidaVencidaPasaAVencidaYSuOperacionAEnMora() {
        Factura f = factura(5L, EstadoFactura.CEDIDA, hoy.minusDays(5), "4000.00");
        DetalleOperacion det = new DetalleOperacion(operacion, f, new BigDecimal("0.00"));
        when(facturas.findByEstadoAndFechaVencimientoBefore(eq(EstadoFactura.CEDIDA), any(LocalDate.class))).thenReturn(List.of(f));
        when(detalles.findByFacturaId(5L)).thenReturn(Optional.of(det));
        when(cupos.findByEstadoAndFechaVencimientoBefore(eq(EstadoCupo.ACTIVO), any(LocalDate.class))).thenReturn(List.of());

        MarcarVencidasRespuesta r = servicio.marcarVencidas();

        assertEquals(EstadoFactura.VENCIDA, f.getEstado());
        assertEquals(EstadoOperacion.EN_MORA, operacion.getEstado());
        assertEquals(List.of("DTE-5"), r.facturasVencidas());
        assertTrue(r.operacionesEnMora().contains("OPF-2026-00001"));
        assertTrue(r.cuposVencidos().isEmpty());
        verify(facturas).save(f);
        verify(operaciones).save(operacion);
    }

    @Test
    void r15CupoActivoVencidoPasaAVencido() {
        when(facturas.findByEstadoAndFechaVencimientoBefore(eq(EstadoFactura.CEDIDA), any(LocalDate.class))).thenReturn(List.of());
        when(cupos.findByEstadoAndFechaVencimientoBefore(eq(EstadoCupo.ACTIVO), any(LocalDate.class))).thenReturn(List.of(cupo));

        MarcarVencidasRespuesta r = servicio.marcarVencidas();

        assertEquals(EstadoCupo.VENCIDO, cupo.getEstado());
        assertEquals(List.of("CUP-001"), r.cuposVencidos());
        assertTrue(r.facturasVencidas().isEmpty());
        verify(cupos).save(cupo);
    }
}