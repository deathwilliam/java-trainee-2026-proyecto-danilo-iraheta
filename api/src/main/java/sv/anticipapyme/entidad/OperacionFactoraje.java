package sv.anticipapyme.entidad;

import jakarta.persistence.*;
import sv.anticipapyme.entidad.enums.EstadoOperacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operacion_factoraje")
public class OperacionFactoraje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true, name = "numero_operacion")
    private String numeroOperacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_cedente_id", nullable = false)
    private Empresa empresaCedente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cupo_pagador_id", nullable = false)
    private CupoPagador cupoPagador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecutivo_id", nullable = false)
    private Ejecutivo ejecutivo;

    @Column(precision = 6, scale = 4, nullable = false, updatable = false, name = "tasa_descuento_aplicada")
    private BigDecimal tasaDescuentoAplicada;

    @Column(precision = 6, scale = 4, nullable = false, updatable = false, name = "tasa_mora_aplicada")
    private BigDecimal tasaMoraAplicada;

    @Column(precision = 15, scale = 2, nullable = false, name = "monto_nominal_total")
    private BigDecimal montoNominalTotal;

    @Column(precision = 15, scale = 2, nullable = false, name = "monto_descuento_total")
    private BigDecimal montoDescuentoTotal;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoOperacion estado;

    @Column(nullable = false, updatable = false, name = "fecha_hora_operacion")
    private LocalDateTime fechaHoraOperacion = LocalDateTime.now();

    protected OperacionFactoraje() {}

    public OperacionFactoraje(String numeroOperacion, Empresa empresaCedente, CupoPagador cupoPagador, Ejecutivo ejecutivo) {
        this.numeroOperacion = numeroOperacion;
        this.empresaCedente = empresaCedente;
        this.cupoPagador = cupoPagador;
        this.ejecutivo = ejecutivo;
        this.tasaDescuentoAplicada = cupoPagador.getCategoriaRiesgo().getTasaDescuentoAnual();
        this.tasaMoraAplicada = cupoPagador.getCategoriaRiesgo().getTasaMoraAnual();
        this.montoNominalTotal = BigDecimal.ZERO;
        this.montoDescuentoTotal = BigDecimal.ZERO;
        this.estado = EstadoOperacion.VIGENTE;
    }

    public Long getId() {
        return id;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public Empresa getEmpresaCedente() {
        return empresaCedente;
    }

    public CupoPagador getCupoPagador() {
        return cupoPagador;
    }

    public Ejecutivo getEjecutivo() {
        return ejecutivo;
    }

    public BigDecimal getTasaDescuentoAplicada() {
        return tasaDescuentoAplicada;
    }

    public BigDecimal getTasaMoraAplicada() {
        return tasaMoraAplicada;
    }

    public BigDecimal getMontoNominalTotal() {
        return montoNominalTotal;
    }

    public BigDecimal getMontoDescuentoTotal() {
        return montoDescuentoTotal;
    }

    public EstadoOperacion getEstado() {
        return estado;
    }

    public LocalDateTime getFechaHoraOperacion() {
        return fechaHoraOperacion;
    }

    public void setMontoNominalTotal(BigDecimal montoNominalTotal) {
        this.montoNominalTotal = montoNominalTotal;
    }

    public void setMontoDescuentoTotal(BigDecimal montoDescuentoTotal) {
        this.montoDescuentoTotal = montoDescuentoTotal;
    }

    public void setEstado(EstadoOperacion estado) {
        this.estado = estado;
    }
}