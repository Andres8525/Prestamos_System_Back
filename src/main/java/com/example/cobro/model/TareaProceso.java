package com.example.cobro.model;

import com.example.cobro.worker.EstadoTarea;
import com.example.cobro.worker.TipoTarea;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Tarea en segundo plano. Se persiste para que la cola sobreviva a un reinicio del servidor
 * y para poder consultar su progreso y su resultado desde el frontend.
 */
@Entity
@Table(name = "tareas_proceso", indexes = @Index(name = "idx_tarea_estado", columnList = "estado"))
public class TareaProceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoTarea tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    /** Mayor número = se atiende antes. */
    private int prioridad;

    private int intentos;

    private int maxIntentos = 3;

    /** Hilo del pool que la procesó. */
    @Column(length = 60)
    private String worker;

    @Column(length = 120)
    private String solicitadaPor;

    /** Resumen legible del resultado. */
    @Column(length = 2000)
    private String resumen;

    /** Contenido generado (por ejemplo, el CSV de un reporte o el texto de un informe). */
    @Column(length = 200000)
    private String resultado;

    @Column(length = 1000)
    private String error;

    @Column(nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    private LocalDateTime iniciadaEn;

    private LocalDateTime finalizadaEn;

    public TareaProceso() {}

    public TareaProceso(TipoTarea tipo, int prioridad, String solicitadaPor) {
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.solicitadaPor = solicitadaPor;
    }

    /** Duración en milisegundos (null si aún no termina). */
    public Long duracionMs() {
        if (iniciadaEn == null || finalizadaEn == null) return null;
        return java.time.Duration.between(iniciadaEn, finalizadaEn).toMillis();
    }

    public Long getId() { return id; }
    public TipoTarea getTipo() { return tipo; }
    public EstadoTarea getEstado() { return estado; }
    public void setEstado(EstadoTarea estado) { this.estado = estado; }
    public int getPrioridad() { return prioridad; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public int getMaxIntentos() { return maxIntentos; }
    public void setMaxIntentos(int maxIntentos) { this.maxIntentos = maxIntentos; }
    public String getWorker() { return worker; }
    public void setWorker(String worker) { this.worker = worker; }
    public String getSolicitadaPor() { return solicitadaPor; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getIniciadaEn() { return iniciadaEn; }
    public void setIniciadaEn(LocalDateTime iniciadaEn) { this.iniciadaEn = iniciadaEn; }
    public LocalDateTime getFinalizadaEn() { return finalizadaEn; }
    public void setFinalizadaEn(LocalDateTime finalizadaEn) { this.finalizadaEn = finalizadaEn; }
}
