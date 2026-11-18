package sv.anticipapyme.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ejecutivo")
public class Ejecutivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false, unique = true, name = "codigo_empleado")
    private String codigoEmpleado;

    @Column(length = 60, nullable = false)
    private String nombres;

    @Column(length = 60, nullable = false)
    private String apellidos;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false, name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Ejecutivo() {}

    public Ejecutivo(String codigoEmpleado, String nombres, String apellidos, Usuario usuario) {
        this.codigoEmpleado = codigoEmpleado;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoEmpleado() {
        return codigoEmpleado;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Boolean getActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}