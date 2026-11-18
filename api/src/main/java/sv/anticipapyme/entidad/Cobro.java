package sv.anticipapyme.entidad;

import jakarta.persistence.*;
import sv.anticipapyme.entidad.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobro")
public class Cobro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true, name = "numero_comprobante")
    private String numeroComprobante;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false, unique = true)
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecutivo_id", nullable = false)
    private Ejecutivo ejecutivo;

    @Column(precision = 15, scale = 2, nullable = false, name = "monto_capital")
    private BigDecimal montoCapital;

    @Column(precision = 15, scale = 2, nullable = false, name = "monto_mora")
    private BigDecimal montoMora = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false, name = "metodo_pago")
    private MetodoPago metodoPago;

    @Column(nullable = false, name = "fecha_hora_pago")
    private LocalDateTime fechaHoraPago;

    protected Cobro() {}

    public Cobro(String numeroComprobante, Factura factura, Ejecutivo ejecutivo, BigDecimal montoMora, MetodoPago metodoPago, LocalDateTime fechaHoraPago) {
        this.numeroComprobante = numeroComprobante;
        this.factura = factura;
        this.ejecutivo = ejecutivo;
        this.montoCapital = factura.getMontoTotal();
        this.montoMora = montoMora;
        this.metodoPago = metodoPago;
        this.fechaHoraPago = fechaHoraPago;
    }

    public Long getId() {
        return id;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public Factura getFactura() {
        return factura;
    }

    public Ejecutivo getEjecutivo() {
        return ejecutivo;
    }

    public BigDecimal getMontoCapital() {
        return montoCapital;
    }

    public BigDecimal getMontoMora() {
        return montoMora;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public LocalDateTime getFechaHoraPago() {
        return fechaHoraPago;
    }
}
