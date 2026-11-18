package sv.anticipapyme.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sv.anticipapyme.dto.CupoRespuesta;
import sv.anticipapyme.dto.CupoSolicitud;
import sv.anticipapyme.entidad.*;
import sv.anticipapyme.entidad.enums.EstadoCupo;
import sv.anticipapyme.entidad.enums.EstadoOperacion;
import sv.anticipapyme.excepcion.DependenciasActivasException;
import sv.anticipapyme.excepcion.OperacionRechazadaException;
import sv.anticipapyme.repositorio.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CupoReglasTest {

    @Mock private CupoPagadorRepositorio cupos;
    @Mock private EmpresaRepositorio empresas;
    @Mock private CategoriaRiesgoRepositorio categorias;
    @Mock private OperacionFactorajeRepositorio operaciones;

    @InjectMocks private CupoServicio servicio;

    private final LocalDate hoy = LocalDate.now();

    private Empresa pagadora;
    private CategoriaRiesgo categoria;
    private Usuario usuario;
    private CupoPagador cupo;

    @BeforeEach
    void preparar() {
        SectorEconomico sector = new SectorEconomico("COM", "Comercio");
        pagadora = conId(new Empresa("nit2", "nrc2", "Supermercados del Valle", "p@p.com", "2223-0000", "SS", sector), 2L);
        categoria = new CategoriaRiesgo("A", "Riesgo bajo", new BigDecimal("0.1800"), new BigDecimal("0.2400"), 120);
        usuario = conId(new Usuario("admin1", "hash", "a@a.com"), 10L);
        cupo = conId(new CupoPagador("CUP-001", pagadora, categoria, usuario,
                new BigDecimal("50000.00"), hoy, hoy.plusDays(180)), 1L);
    }

    private static <T> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }

    private CupoSolicitud solicitud() {
        return new CupoSolicitud("CUP-002", 2L, 1L, new BigDecimal("20000.00"), hoy, hoy.plusDays(180));
    }

    @Test
    void aprobarCasoBuenoCreaElCupo() {
        when(empresas.findById(2L)).thenReturn(Optional.of(pagadora));
        when(cupos.existsByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(false);
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(cupos.save(any(CupoPagador.class))).thenAnswer(inv -> inv.getArgument(0));

        CupoRespuesta r = servicio.aprobar(solicitud(), usuario);

        assertEquals("CUP-002", r.numeroCupo());
        assertEquals("ACTIVO", r.estado());
        assertEquals(new BigDecimal("20000.00"), r.montoDisponible());
        verify(cupos).save(any(CupoPagador.class));
    }

    @Test
    void fechaVencimientoNoPosteriorSeRechaza() {
        CupoSolicitud mala = new CupoSolicitud("CUP-003", 2L, 1L, new BigDecimal("20000.00"), hoy, hoy);

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.aprobar(mala, usuario));

        assertEquals("La fecha de vencimiento debe ser posterior a la de aprobacion", e.getMessage());
        verifyNoInteractions(empresas, categorias);
        verify(cupos, never()).save(any());
    }

    @Test
    void desactivarConOperacionesVigentesSeRechaza() {
        when(cupos.findById(1L)).thenReturn(Optional.of(cupo));
        when(operaciones.existsByCupoPagadorIdAndEstado(1L, EstadoOperacion.VIGENTE)).thenReturn(true);

        DependenciasActivasException e = assertThrows(DependenciasActivasException.class,
                () -> servicio.desactivar(1L));

        assertEquals("El cupo CUP-001 respalda operaciones VIGENTES y no se puede desactivar", e.getMessage());
        verify(cupos, never()).save(any());
    }

    @Test
    void r3PagadoraYaTieneCupoActivoSeRechaza() {
        when(empresas.findById(2L)).thenReturn(Optional.of(pagadora));
        when(cupos.existsByEmpresaPagadoraIdAndEstado(2L, EstadoCupo.ACTIVO)).thenReturn(true);

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.aprobar(solicitud(), usuario));

        assertEquals("La empresa Supermercados del Valle ya tiene un cupo ACTIVO", e.getMessage());
        verify(cupos, never()).save(any());
    }
}
