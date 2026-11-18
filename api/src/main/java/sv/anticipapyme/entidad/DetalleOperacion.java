package sv.anticipapyme.entidad;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_operacion")
public class DetalleOperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacion_factoraje_id", nullable = false)
    private OperacionFactoraje operacionFactoraje;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false, unique = true)
    private Factura factura;

    @Column(precision = 15, scale = 2, nullable = false, name = "monto_descuento")
    private BigDecimal montoDescuento;

    protected DetalleOperacion() {}

    public DetalleOperacion(OperacionFactoraje operacionFactoraje, Factura factura, BigDecimal montoDescuento) {
        this.operacionFactoraje = operacionFactoraje;
        this.factura = factura;
        this.montoDescuento = montoDescuento;
    }

    public Long getId() {
        return id;
    }

    public OperacionFactoraje getOperacionFactoraje() {
        return operacionFactoraje;
    }

    public Factura getFactura() {
        return factura;
    }

    public BigDecimal getMontoDescuento() {
        return montoDescuento;
    }
}