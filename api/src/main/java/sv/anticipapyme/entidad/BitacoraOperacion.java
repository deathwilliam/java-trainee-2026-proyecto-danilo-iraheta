package sv.anticipapyme.entidad;

import jakarta.persistence.*;
import sv.anticipapyme.entidad.enums.ResultadoOperacion;
import sv.anticipapyme.entidad.enums.TipoOperacion;

import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_operacion")
public class BitacoraOperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operacion_factoraje_id")
    private OperacionFactoraje operacionFactoraje;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false, name = "tipo_operacion")
    private TipoOperacion tipoOperacion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ResultadoOperacion resultado;

    @Column(length = 255, nullable = false)
    private String detalle;

    @Column(nullable = false, updatable = false, name = "fecha_hora")
    private LocalDateTime fechaHora = LocalDateTime.now();

    protected BitacoraOperacion() {}

    public BitacoraOperacion(Usuario usuario, OperacionFactoraje operacionFactoraje, TipoOperacion tipoOperacion, ResultadoOperacion resultado, String detalle) {
        this.usuario = usuario;
        this.operacionFactoraje = operacionFactoraje;
        this.tipoOperacion = tipoOperacion;
        this.resultado = resultado;
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public OperacionFactoraje getOperacionFactoraje() {
        return operacionFactoraje;
    }

    public TipoOperacion getTipoOperacion() {
        return tipoOperacion;
    }

    public ResultadoOperacion getResultado() {
        return resultado;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}