package sv.anticipapyme.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_rol",
        uniqueConstraints =
            @UniqueConstraint(name = "uk_usuario_rol", columnNames = {"usuario_id", "rol_id"}))
public class UsuarioRol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false, updatable = false, name = "asignado_en")
    private LocalDateTime asignadoEn = LocalDateTime.now();

    protected UsuarioRol() {}

    public UsuarioRol(Usuario usuario, Rol rol) {
        this.usuario = usuario;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return rol;
    }

    public LocalDateTime getAsignadoEn() {
        return asignadoEn;
    }
}