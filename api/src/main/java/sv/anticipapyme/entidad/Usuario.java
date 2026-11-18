package sv.anticipapyme.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 100, nullable = false, name = "password_hash")
    private String passwordHash;

    @Column(length = 100, nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false, name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Usuario() {}

    public Usuario(String username, String passwordHash, String correo) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.correo = correo;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getCorreo() {
        return correo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
