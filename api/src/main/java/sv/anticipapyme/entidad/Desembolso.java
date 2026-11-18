package sv.anticipapyme.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "desembolso")
public class Desembolso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true, name = "numero_comprobante")
    private String numeroComprobante;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacion_factoraje_id", nullable = false, unique = true)
    private OperacionFactoraje operacionFactoraje;

    @Column(length = 30, nullable = false, updatable = false, name = "cuenta_destino")
    private String cuentaDestino;

    @Column(nullable = false, updatable = false, name = "fecha_hora")
    private LocalDateTime fechaHora = LocalDateTime.now();

    protected Desembolso() {}

    public Desembolso(String numeroComprobante, OperacionFactoraje operacionFactoraje) {
        this.numeroComprobante = numeroComprobante;
        this.operacionFactoraje = operacionFactoraje;
        this.cuentaDestino = operacionFactoraje.getEmpresaCedente().getCuentaBancaria();
    }

    public Long getId() {
        return id;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public OperacionFactoraje getOperacionFactoraje() {
        return operacionFactoraje;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
