package sv.anticipapyme.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 17, nullable = false, unique = true)
    private String nit;

    @Column(length = 10, nullable = false, unique = true)
    private String nrc;

    @Column(length = 150, nullable = false, name = "nombre_legal")
    private String nombreLegal;

    @Column(length = 100, name = "nombre_comercial")
    private String nombreComercial;

    @Column(length = 100, nullable = false)
    private String correo;

    @Column(length = 15, nullable = false)
    private String telefono;

    @Column(length = 200, nullable = false)
    private String direccion;

    @Column(length = 30, name = "cuenta_bancaria")
    private String cuentaBancaria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_economico_id", nullable = false)
    private SectorEconomico sectorEconomico;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false, name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Empresa() {}

    public Empresa(String nit, String nrc, String nombreLegal, String correo, String telefono, String direccion, SectorEconomico sectorEconomico) {
        this.nit = nit;
        this.nrc = nrc;
        this.nombreLegal = nombreLegal;
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
        this.sectorEconomico = sectorEconomico;
    }

    public Long getId() {
        return id;
    }

    public String getNit() {
        return nit;
    }

    public String getNrc() {
        return nrc;
    }

    public String getNombreLegal() {
        return nombreLegal;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCuentaBancaria() {
        return cuentaBancaria;
    }

    public SectorEconomico getSectorEconomico() {
        return sectorEconomico;
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

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setCuentaBancaria(String cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public void setSectorEconomico(SectorEconomico sectorEconomico) {
        this.sectorEconomico = sectorEconomico;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}