package com.example.cobro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro de cada entrenamiento del modelo de scoring (para auditar su evolución en el tiempo). */
@Entity
@Table(name = "entrenamientos_modelo")
public class EntrenamientoModelo {

    /** Qué disparó el entrenamiento. */
    public enum Origen { INICIO, MANUAL, PROGRAMADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Origen origen;

    @Column(nullable = false)
    private String algoritmo;

    @Column(nullable = false)
    private Integer muestrasReales;

    @Column(nullable = false)
    private Integer muestrasSinteticas;

    @Column(nullable = false)
    private Double aucPrueba;

    @Column(nullable = false)
    private Double exactitudPrueba;

    @Column(nullable = false)
    private Double precisionPrueba;

    @Column(nullable = false)
    private Double sensibilidadPrueba;

    @Column(nullable = false)
    private Double f1Prueba;

    @Column(nullable = false)
    private Double aucValidacion;

    @Column(nullable = false)
    private Double aucDesviacion;

    /** Resumen de la comparación de algoritmos, ej. "Regresión logística=0.912; Árbol=0.887". */
    @Column(length = 500)
    private String comparacion;

    public EntrenamientoModelo() {}

    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Origen getOrigen() { return origen; }
    public void setOrigen(Origen origen) { this.origen = origen; }
    public String getAlgoritmo() { return algoritmo; }
    public void setAlgoritmo(String algoritmo) { this.algoritmo = algoritmo; }
    public Integer getMuestrasReales() { return muestrasReales; }
    public void setMuestrasReales(Integer muestrasReales) { this.muestrasReales = muestrasReales; }
    public Integer getMuestrasSinteticas() { return muestrasSinteticas; }
    public void setMuestrasSinteticas(Integer muestrasSinteticas) { this.muestrasSinteticas = muestrasSinteticas; }
    public Double getAucPrueba() { return aucPrueba; }
    public void setAucPrueba(Double aucPrueba) { this.aucPrueba = aucPrueba; }
    public Double getExactitudPrueba() { return exactitudPrueba; }
    public void setExactitudPrueba(Double exactitudPrueba) { this.exactitudPrueba = exactitudPrueba; }
    public Double getPrecisionPrueba() { return precisionPrueba; }
    public void setPrecisionPrueba(Double precisionPrueba) { this.precisionPrueba = precisionPrueba; }
    public Double getSensibilidadPrueba() { return sensibilidadPrueba; }
    public void setSensibilidadPrueba(Double sensibilidadPrueba) { this.sensibilidadPrueba = sensibilidadPrueba; }
    public Double getF1Prueba() { return f1Prueba; }
    public void setF1Prueba(Double f1Prueba) { this.f1Prueba = f1Prueba; }
    public Double getAucValidacion() { return aucValidacion; }
    public void setAucValidacion(Double aucValidacion) { this.aucValidacion = aucValidacion; }
    public Double getAucDesviacion() { return aucDesviacion; }
    public void setAucDesviacion(Double aucDesviacion) { this.aucDesviacion = aucDesviacion; }
    public String getComparacion() { return comparacion; }
    public void setComparacion(String comparacion) { this.comparacion = comparacion; }
}
