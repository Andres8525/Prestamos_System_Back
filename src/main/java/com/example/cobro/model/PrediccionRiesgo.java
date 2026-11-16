package com.example.cobro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Predicción de riesgo registrada para un cliente: permite ver la evolución de su riesgo en el tiempo. */
@Entity
@Table(name = "predicciones_riesgo", indexes = @Index(name = "idx_prediccion_cliente", columnList = "cliente_id, fecha"))
public class PrediccionRiesgo {

    public enum Origen {
        /** Al aprobar un préstamo o renovación. */
        CREDITO_APROBADO,
        /** La IA bloqueó la renovación por riesgo alto. */
        CREDITO_BLOQUEADO,
        /** Seguimiento automático diario de la cartera activa. */
        SEGUIMIENTO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private Double probabilidad;

    @Column(nullable = false, length = 10)
    private String nivelRiesgo;

    @Column(nullable = false)
    private String algoritmo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Origen origen;

    public PrediccionRiesgo() {}

    public PrediccionRiesgo(Cliente cliente, double probabilidad, String nivelRiesgo,
                            String algoritmo, Origen origen) {
        this.cliente = cliente;
        this.fecha = LocalDateTime.now();
        this.probabilidad = probabilidad;
        this.nivelRiesgo = nivelRiesgo;
        this.algoritmo = algoritmo;
        this.origen = origen;
    }

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public LocalDateTime getFecha() { return fecha; }
    public Double getProbabilidad() { return probabilidad; }
    public String getNivelRiesgo() { return nivelRiesgo; }
    public String getAlgoritmo() { return algoritmo; }
    public Origen getOrigen() { return origen; }
}
