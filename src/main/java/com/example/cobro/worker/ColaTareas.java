package com.example.cobro.worker;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cola en memoria de tareas pendientes (patrón productor-consumidor).
 * Los productores (API, programador) encolan identificadores; los workers los toman con {@link #tomar}.
 * Se ordena por prioridad (mayor primero) y, a igual prioridad, por orden de llegada.
 * La fuente de verdad es la tabla {@code tareas_proceso}: al reiniciar, las pendientes se vuelven a encolar.
 */
@Component
public class ColaTareas {

    record Entrada(long tareaId, int prioridad, long secuencia) {}

    private final AtomicLong secuencia = new AtomicLong();
    private final PriorityBlockingQueue<Entrada> cola = new PriorityBlockingQueue<>(64,
            Comparator.comparingInt(Entrada::prioridad).reversed().thenComparingLong(Entrada::secuencia));

    public void encolar(long tareaId, int prioridad) {
        cola.put(new Entrada(tareaId, prioridad, secuencia.incrementAndGet()));
    }

    /** Espera hasta {@code esperaMs} por una tarea; devuelve null si no llegó ninguna. */
    public Long tomar(long esperaMs) throws InterruptedException {
        Entrada e = cola.poll(esperaMs, TimeUnit.MILLISECONDS);
        return e == null ? null : e.tareaId();
    }

    public boolean quitar(long tareaId) {
        return cola.removeIf(e -> e.tareaId() == tareaId);
    }

    public int tamano() {
        return cola.size();
    }
}
