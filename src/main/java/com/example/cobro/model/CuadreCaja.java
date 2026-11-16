package com.example.cobro.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cierre (cuadre) de caja diario de un cobrador.
 * <pre>
 *   efectivoEsperado = baseInicial + totalCobrado - totalPrestado - gastos
 *   diferencia       = efectivoEntregado - efectivoEsperado   (&gt;0 sobrante, &lt;0 faltante)
 * </pre>
 */
@Entity
@Table(name = "cuadres_caja",
       uniqueConstraints = @UniqueConstraint(name = "uk_cuadre_cobrador_fecha", columnNames = {"cobrador_id", "fecha"}))
public class CuadreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cobrador_id", nullable = false)
    private Cobrador cobrador;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Double baseInicial;

    @Column(nullable = false)
    private Double totalCobrado;

    @Column(nullable = false)
    private Integer numeroAbonos;

    @Column(nullable = false)
    private Double totalPrestado;

    @Column(nullable = false)
    private Integer numeroPrestamos;

    @Column(nullable = false)
    private Double gastos;

    @Column(nullable = false)
    private Double efectivoEsperado;

    @Column(nullable = false)
    private Double efectivoEntregado;

    @Column(nullable = false)
    private Double diferencia;

    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fechaCierre;

    public CuadreCaja() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cobrador getCobrador() { return cobrador; }
    public void setCobrador(Cobrador cobrador) { this.cobrador = cobrador; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getBaseInicial() { return baseInicial; }
    public void setBaseInicial(Double baseInicial) { this.baseInicial = baseInicial; }

    public Double getTotalCobrado() { return totalCobrado; }
    public void setTotalCobrado(Double totalCobrado) { this.totalCobrado = totalCobrado; }

    public Integer getNumeroAbonos() { return numeroAbonos; }
    public void setNumeroAbonos(Integer numeroAbonos) { this.numeroAbonos = numeroAbonos; }

    public Double getTotalPrestado() { return totalPrestado; }
    public void setTotalPrestado(Double totalPrestado) { this.totalPrestado = totalPrestado; }

    public Integer getNumeroPrestamos() { return numeroPrestamos; }
    public void setNumeroPrestamos(Integer numeroPrestamos) { this.numeroPrestamos = numeroPrestamos; }

    public Double getGastos() { return gastos; }
    public void setGastos(Double gastos) { this.gastos = gastos; }

    public Double getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(Double efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }

    public Double getEfectivoEntregado() { return efectivoEntregado; }
    public void setEfectivoEntregado(Double efectivoEntregado) { this.efectivoEntregado = efectivoEntregado; }

    public Double getDiferencia() { return diferencia; }
    public void setDiferencia(Double diferencia) { this.diferencia = diferencia; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
}
