package sv.anticipapyme.servicio;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import sv.anticipapyme.entidad.DetalleOperacion;
import sv.anticipapyme.entidad.OperacionFactoraje;
import sv.anticipapyme.entidad.Usuario;
import sv.anticipapyme.entidad.enums.EstadoFactura;
import sv.anticipapyme.excepcion.FacturaNoConfirmadaException;
import sv.anticipapyme.repositorio.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

@SpringBootTest(properties = {
        // use el puerto 5433 porque en el 5432 ya tenía otra instancia de postgresql de otra version que uso en la universidad
        "spring.datasource.url=jdbc:postgresql://localhost:${DB_PUERTO_HOST:5432}/anticipapyme_test",
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.clave=clave-de-prueba-anticipapyme-para-tests-0123456789"
})
class AnticipoServicioTest {

    private static final Long CEDENTE = 1L;
    private static final Long CUPO = 1L;

    @Autowired
    private AnticipoServicio anticipos;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private FacturaRepositorio facturas;

    @Autowired
    private CupoPagadorRepositorio cupos;

    @Autowired
    private OperacionFactorajeRepositorio operaciones;

    @Autowired
    private DesembolsoRepositorio desembolsos;

    @Autowired
    private BitacoraOperacionRepositorio bitacoras;

    @MockitoSpyBean
    private DetalleOperacionRepositorio detalles;

    @Test
    void siFallaElTercerDetalleNoQuedaNada() {
        Usuario asesor = asesor();
        BigDecimal disponibleAntes = disponible();
        long operacionesAntes = operaciones.count();
        long detallesAntes = detalles.count();
        long desembolsosAntes = desembolsos.count();
        long bitacorasAntes = bitacoras.count();

        Answer<?> loDeSiempre = mockingDetails(detalles).getMockCreationSettings().getDefaultAnswer();
        doAnswer(loDeSiempre)
                .doAnswer(loDeSiempre)
                .doThrow(new IllegalStateException("Falla simulada al grabar el tercer detalle"))
                .when(detalles).save(any(DetalleOperacion.class));

        assertThrows(IllegalStateException.class,
                () -> anticipos.anticipar(CEDENTE, List.of(1L, 2L, 3L), asesor));

        assertEquals(operacionesAntes, operaciones.count(), "quedo la operacion");
        assertEquals(detallesAntes, detalles.count(), "quedaron detalles sueltos");
        assertEquals(desembolsosAntes, desembolsos.count(), "quedo un desembolso");

        for (Long id : List.of(1L, 2L, 3L)) {
            assertEquals(EstadoFactura.CONFIRMADA, estado(id), "la factura " + id + " cambio de estado");
        }
        assertEquals(0, disponibleAntes.compareTo(disponible()), "el cupo cambio");

        assertEquals(bitacorasAntes + 1, bitacoras.count(), "no quedo el FALLIDO en la bitacora");
    }

    @Test
    void siUnaReglaDiceQueNoNoSeTocaNada() {
        Usuario asesor = asesor();
        BigDecimal disponibleAntes = disponible();
        long operacionesAntes = operaciones.count();
        long detallesAntes = detalles.count();
        long bitacorasAntes = bitacoras.count();

        assertThrows(FacturaNoConfirmadaException.class,
                () -> anticipos.anticipar(CEDENTE, List.of(4L), asesor));

        assertEquals(operacionesAntes, operaciones.count());
        assertEquals(detallesAntes, detalles.count());
        assertEquals(EstadoFactura.REGISTRADA, estado(4L));
        assertEquals(0, disponibleAntes.compareTo(disponible()));
        assertEquals(bitacorasAntes + 1, bitacoras.count(), "no quedo el FALLIDO en la bitacora");
    }

    @Test
    void siTodoSaleBienQuedanTodasLasPartes() {
        Usuario asesor = asesor();
        BigDecimal disponibleAntes = disponible();
        long operacionesAntes = operaciones.count();
        long detallesAntes = detalles.count();
        long desembolsosAntes = desembolsos.count();
        long bitacorasAntes = bitacoras.count();

        // F5: 4000.00 a 45 dias -> 90.00 | F6: 1000.00 a 20 dias -> 10.00 (tasa 18%)
        OperacionFactoraje operacion = anticipos.anticipar(CEDENTE, List.of(5L, 6L), asesor);

        assertEquals(0, new BigDecimal("5000.00").compareTo(operacion.getMontoNominalTotal()), "R11");
        assertEquals(0, new BigDecimal("100.00").compareTo(operacion.getMontoDescuentoTotal()), "R10 y R12");

        assertEquals(operacionesAntes + 1, operaciones.count());
        assertEquals(detallesAntes + 2, detalles.count());
        assertEquals(desembolsosAntes + 1, desembolsos.count());
        assertEquals(bitacorasAntes + 1, bitacoras.count(), "falta el EXITOSO");

        assertEquals(EstadoFactura.CEDIDA, estado(5L));
        assertEquals(EstadoFactura.CEDIDA, estado(6L));
        assertEquals(0, disponibleAntes.subtract(new BigDecimal("5000.00")).compareTo(disponible()), "R13");
    }

    private Usuario asesor() {
        return usuarios.findByUsername("asesor1").orElseThrow();
    }

    private BigDecimal disponible() {
        return cupos.findById(CUPO).orElseThrow().getMontoDisponible();
    }

    private EstadoFactura estado(Long facturaId) {
        return facturas.findById(facturaId).orElseThrow().getEstado();
    }
}