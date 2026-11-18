package sv.anticipapyme.entidad;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "categoria_riesgo")
public class CategoriaRiesgo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, unique = true, nullable = false)
    private String codigo;

    @Column(length = 80, nullable = false)
    private String descripcion;

    @Column(precision = 6, scale = 4, nullable = false, name = "tasa_descuento_anual")
    private BigDecimal tasaDescuentoAnual;

    @Column(precision = 6, scale = 4, nullable = false, name = "tasa_mora_anual")
    private BigDecimal tasaMoraAnual;

    @Column(nullable = false, name = "plazo_maximo_dias")
    private Integer plazoMaximoDias;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected CategoriaRiesgo() {}

    public CategoriaRiesgo(String codigo, String descripcion, BigDecimal tasaDescuentoAnual, BigDecimal tasaMoraAnual, Integer plazoMaximoDias) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tasaDescuentoAnual = tasaDescuentoAnual;
        this.tasaMoraAnual = tasaMoraAnual;
        this.plazoMaximoDias = plazoMaximoDias;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getTasaDescuentoAnual() {
        return tasaDescuentoAnual;
    }

    public BigDecimal getTasaMoraAnual() {
        return tasaMoraAnual;
    }

    public Integer getPlazoMaximoDias() {
        return plazoMaximoDias;
    }

    public Boolean getActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setTasaDescuentoAnual(BigDecimal tasaDescuentoAnual) {
        this.tasaDescuentoAnual = tasaDescuentoAnual;
    }

    public void setTasaMoraAnual(BigDecimal tasaMoraAnual) {
        this.tasaMoraAnual = tasaMoraAnual;
    }

    public void setPlazoMaximoDias(Integer plazoMaximoDias) {
        this.plazoMaximoDias = plazoMaximoDias;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
