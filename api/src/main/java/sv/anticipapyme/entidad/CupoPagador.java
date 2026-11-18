package sv.anticipapyme.entidad;

import jakarta.persistence.*;
import sv.anticipapyme.entidad.enums.EstadoCupo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupo_pagador")
public class CupoPagador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true, name = "numero_cupo")
    private String numeroCupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_pagadora_id", nullable = false)
    private Empresa empresaPagadora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_riesgo_id", nullable = false)
    private CategoriaRiesgo categoriaRiesgo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aprobado_por_usuario_id", nullable = false)
    private Usuario aprobadoPorUsuario;

    @Column(precision = 15, scale = 2, nullable = false, name = "monto_aprobado")
    private BigDecimal montoAprobado;

    @Column(precision = 15, scale = 2, nullable = false, name = "monto_disponible")
    private BigDecimal montoDisponible;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoCupo estado;

    @Column(nullable = false, name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    @Column(nullable = false, name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(nullable = false, updatable = false, name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected CupoPagador() {}

    public CupoPagador(String numeroCupo, Empresa empresaPagadora, CategoriaRiesgo categoriaRiesgo, Usuario aprobadoPorUsuario, BigDecimal montoAprobado, LocalDate fechaAprobacion, LocalDate fechaVencimiento) {
        this.numeroCupo = numeroCupo;
        this.empresaPagadora = empresaPagadora;
        this.categoriaRiesgo = categoriaRiesgo;
        this.aprobadoPorUsuario = aprobadoPorUsuario;
        this.montoAprobado = montoAprobado;
        this.montoDisponible = montoAprobado;
        this.fechaAprobacion = fechaAprobacion;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = EstadoCupo.ACTIVO;
    }

    public Long getId() {
        return id;
    }

    public String getNumeroCupo() {
        return numeroCupo;
    }

    public Empresa getEmpresaPagadora() {
        return empresaPagadora;
    }

    public CategoriaRiesgo getCategoriaRiesgo() {
        return categoriaRiesgo;
    }

    public Usuario getAprobadoPorUsuario() {
        return aprobadoPorUsuario;
    }

    public BigDecimal getMontoAprobado() {
        return montoAprobado;
    }

    public BigDecimal getMontoDisponible() {
        return montoDisponible;
    }

    public EstadoCupo getEstado() {
        return estado;
    }

    public LocalDate getFechaAprobacion() {
        return fechaAprobacion;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setMontoDisponible(BigDecimal montoDisponible) {
        this.montoDisponible = montoDisponible;
    }

    public void setEstado(EstadoCupo estado) {
        this.estado = estado;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}