package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.MarcarVencidasRespuesta;
import sv.anticipapyme.entidad.CupoPagador;
import sv.anticipapyme.entidad.DetalleOperacion;
import sv.anticipapyme.entidad.Factura;
import sv.anticipapyme.entidad.OperacionFactoraje;
import sv.anticipapyme.entidad.enums.EstadoCupo;
import sv.anticipapyme.entidad.enums.EstadoFactura;
import sv.anticipapyme.entidad.enums.EstadoOperacion;
import sv.anticipapyme.repositorio.CupoPagadorRepositorio;
import sv.anticipapyme.repositorio.DetalleOperacionRepositorio;
import sv.anticipapyme.repositorio.FacturaRepositorio;
import sv.anticipapyme.repositorio.OperacionFactorajeRepositorio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class VencimientoServicio {

    private final FacturaRepositorio facturas;
    private final CupoPagadorRepositorio cupos;
    private final DetalleOperacionRepositorio detalles;
    private final OperacionFactorajeRepositorio operaciones;

    public VencimientoServicio(FacturaRepositorio facturas, CupoPagadorRepositorio cupos,
                               DetalleOperacionRepositorio detalles, OperacionFactorajeRepositorio operaciones) {
        this.facturas = facturas;
        this.cupos = cupos;
        this.detalles = detalles;
        this.operaciones = operaciones;
    }

    @Transactional
    public MarcarVencidasRespuesta marcarVencidas() {
        LocalDate hoy = LocalDate.now();

        List<String> facturasVencidas = new ArrayList<>();
        Set<String> operacionesEnMora = new LinkedHashSet<>();
        for (Factura factura : facturas.findByEstadoAndFechaVencimientoBefore(EstadoFactura.CEDIDA, hoy)) {
            factura.setEstado(EstadoFactura.VENCIDA);
            facturas.save(factura);
            facturasVencidas.add(factura.getCodigoGeneracion());

            DetalleOperacion detalle = detalles.findByFacturaId(factura.getId()).orElse(null);
            if (detalle != null) {
                OperacionFactoraje operacion = detalle.getOperacionFactoraje();
                operacion.setEstado(EstadoOperacion.EN_MORA);
                operaciones.save(operacion);
                operacionesEnMora.add(operacion.getNumeroOperacion());
            }
        }

        List<String> cuposVencidos = new ArrayList<>();
        for (CupoPagador cupo : cupos.findByEstadoAndFechaVencimientoBefore(EstadoCupo.ACTIVO, hoy)) {
            cupo.setEstado(EstadoCupo.VENCIDO);
            cupos.save(cupo);
            cuposVencidos.add(cupo.getNumeroCupo());
        }

        return new MarcarVencidasRespuesta(facturasVencidas, new ArrayList<>(operacionesEnMora), cuposVencidos);
    }
}