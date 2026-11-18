package sv.anticipapyme.entidad;

import jakarta.persistence.*;
import sv.anticipapyme.entidad.enums.EstadoFactura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura")
public class Factura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, nullable = false, unique = true, name = "codigo_generacion")
    private String codigoGeneracion;

    @Column(length = 40, nullable = false, name = "sello_recepcion")
    private String selloRecepcion;

    @Column(precision = 15, scale = 2, nullable = false, name = "monto_total")
    private BigDecimal montoTotal;

    @Column(nullable = false, name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(nullable = false, name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_cedente_id", nullable = false)
    private Empresa empresaCedente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_pagadora_id", nullable = false)
    private Empresa empresaPagadora;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoFactura estado;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(nullable = false, updatable = false, name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Factura() {}

    public Factura(String codigoGeneracion, String selloRecepcion, BigDecimal montoTotal, LocalDate fechaEmision, LocalDate fechaVencimiento, Empresa empresaCedente, Empresa empresaPagadora) {
        this.codigoGeneracion = codigoGeneracion;
        this.selloRecepcion = selloRecepcion;
        this.montoTotal = montoTotal;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.empresaCedente = empresaCedente;
        this.empresaPagadora = empresaPagadora;
        this.estado = EstadoFactura.REGISTRADA;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoGeneracion() {
        return codigoGeneracion;
    }

    public String getSelloRecepcion() {
        return selloRecepcion;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public Empresa getEmpresaCedente() {
        return empresaCedente;
    }

    public Empresa getEmpresaPagadora() {
        return empresaPagadora;
    }

    public EstadoFactura getEstado() {
        return estado;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setEstado(EstadoFactura estado) {
        this.estado = estado;
    }

    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }
}