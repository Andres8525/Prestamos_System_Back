package com.example.cobro.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pool de workers: N hilos consumen la {@link ColaTareas} y ejecutan cada tarea con el
 * {@link ManejadorTarea} de su tipo. Un intento fallido se reintenta con espera exponencial
 * (2 s, 4 s, 8 s…) hasta el máximo de intentos de la tarea.
 *
 * <p>El número de hilos es configurable ({@code app.workers.hilos}); en el plan gratuito de Render
 * conviene dejar 2 para no superar los 512 MB de memoria.</p>
 */
@Component
public class WorkerPool implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);
    private static final long ESPERA_COLA_MS = 1000;
    private static final long BACKOFF_BASE_MS = 2000;

    private final ColaTareas cola;
    private final TareaService tareas;
    private final Map<TipoTarea, ManejadorTarea> manejadores = new EnumMap<>(TipoTarea.class);
    private final int numeroHilos;
    private final boolean habilitado;

    private final List<Thread> hilos = new ArrayList<>();
    private final ScheduledExecutorService reintentos = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "worker-reintentos");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger ocupados = new AtomicInteger();
    private final AtomicLong completadas = new AtomicLong();
    private final AtomicLong fallidas = new AtomicLong();
    private volatile boolean corriendo;

    public WorkerPool(ColaTareas cola, TareaService tareas, List<ManejadorTarea> lista,
                      @Value("${app.workers.hilos:2}") int numeroHilos,
                      @Value("${app.workers.habilitado:true}") boolean habilitado) {
        this.cola = cola;
        this.tareas = tareas;
        this.numeroHilos = Math.max(1, numeroHilos);
        this.habilitado = habilitado;
        lista.forEach(m -> manejadores.put(m.tipo(), m));
    }

    @Override
    public synchronized void start() {
        if (!habilitado || corriendo) return;
        corriendo = true;
        int recuperadas = 0;
        try {
            recuperadas = tareas.recuperarPendientes();
        } catch (Exception e) {
            log.warn("No se pudieron recuperar las tareas pendientes: {}", e.getMessage());
        }
        for (int i = 1; i <= numeroHilos; i++) {
            Thread hilo = new Thread(this::bucle, "worker-" + i);
            hilo.setDaemon(true);
            hilo.start();
            hilos.add(hilo);
        }
        log.info("Pool de workers iniciado: {} hilos, {} tareas recuperadas, tipos {}",
                numeroHilos, recuperadas, manejadores.keySet());
    }

    @Override
    public synchronized void stop() {
        corriendo = false;
        hilos.forEach(Thread::interrupt);
        for (Thread h : hilos) {
            try { h.join(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        hilos.clear();
        reintentos.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return corriendo;
    }

    /** Arranca después de JPA y del resto de beans. */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    private void bucle() {
        while (corriendo) {
            try {
                Long id = cola.tomar(ESPERA_COLA_MS);
                if (id != null) procesar(id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Error inesperado en {}: {}", Thread.currentThread().getName(), e.getMessage());
            }
        }
    }

    void procesar(long id) {
        var tarea = tareas.iniciar(id, Thread.currentThread().getName());
        if (tarea.isEmpty()) return; // cancelada o ya atendida
        ManejadorTarea manejador = manejadores.get(tarea.get().getTipo());
        ocupados.incrementAndGet();
        try {
            if (manejador == null) {
                throw new IllegalStateException("No hay worker para " + tarea.get().getTipo());
            }
            tareas.completar(id, manejador.ejecutar());
            completadas.incrementAndGet();
        } catch (Exception e) {
            fallidas.incrementAndGet();
            boolean reintentar = tareas.fallar(id, e);
            log.warn("Tarea {} ({}) falló en el intento {}: {}", id, tarea.get().getTipo(),
                    tarea.get().getIntentos(), e.getMessage());
            if (reintentar) {
                long espera = BACKOFF_BASE_MS << Math.min(tarea.get().getIntentos() - 1, 5);
                reintentos.schedule(() -> cola.encolar(id, tarea.get().getPrioridad()), espera, TimeUnit.MILLISECONDS);
            }
        } finally {
            ocupados.decrementAndGet();
        }
    }

    public boolean soporta(TipoTarea tipo) {
        return manejadores.containsKey(tipo);
    }

    public int getNumeroHilos() { return numeroHilos; }
    public int getOcupados() { return ocupados.get(); }
    public long getCompletadas() { return completadas.get(); }
    public long getFallidas() { return fallidas.get(); }
    public int getEnCola() { return cola.tamano(); }
}
