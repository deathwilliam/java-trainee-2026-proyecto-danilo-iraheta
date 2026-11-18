package sv.anticipapyme.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sector_economico")
public class SectorEconomico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true)
    private String codigo;

    @Column(length = 80, nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false, name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected SectorEconomico() {}

    public SectorEconomico(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Boolean getActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
