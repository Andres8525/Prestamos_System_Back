package com.example.cobro.worker;

import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.TareaProceso;
import com.example.cobro.repository.TareaProcesoRepository;
import com.example.cobro.tiemporeal.EventoNegocio;
import com.example.cobro.tiemporeal.TipoEvento;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Operaciones sobre las tareas en segundo plano: encolar, transiciones de estado, reintentos y consultas.
 * Cada transición es una transacción corta; el trabajo pesado lo hace el worker fuera de ella.
 */
@Service
public class TareaService {

    private static final int LARGO_ERROR = 1000;

    private final TareaProcesoRepository repositorio;
    private final ColaTareas cola;
    private final ApplicationEventPublisher eventos;

    public TareaService(TareaProcesoRepository repositorio, ColaTareas cola, ApplicationEventPublisher eventos) {
        this.repositorio = repositorio;
        this.cola = cola;
        this.eventos = eventos;
    }

    private void anunciar(TareaProceso t) {
        eventos.publishEvent(EventoNegocio.de(TipoEvento.TAREA_ACTUALIZADA,
                t.getTipo().getNombre() + ": " + t.getEstado(),
                "tareaId", t.getId(), "tipo", t.getTipo().name(), "estado", t.getEstado().name(),
                "intentos", t.getIntentos(), "worker", t.getWorker(), "resumen", t.getResumen(), "error", t.getError()));
    }

    /** Registra la tarea como PENDIENTE y la pone en la cola de los workers. */
    public TareaProceso encolar(TipoTarea tipo, int prioridad, String solicitante) {
        TareaProceso tarea = repositorio.save(new TareaProceso(tipo, Math.max(0, Math.min(prioridad, 10)), solicitante));
        cola.encolar(tarea.getId(), tarea.getPrioridad());
        anunciar(tarea);
        return tarea;
    }

    /** El worker toma la tarea: solo si sigue PENDIENTE (pudo cancelarse mientras esperaba). */
    @Transactional
    public Optional<TareaProceso> iniciar(long id, String worker) {
        return repositorio.findById(id)
                .filter(t -> t.getEstado() == EstadoTarea.PENDIENTE)
                .map(t -> {
                    t.setEstado(EstadoTarea.EN_PROCESO);
                    t.setIntentos(t.getIntentos() + 1);
                    t.setWorker(worker);
                    t.setIniciadaEn(LocalDateTime.now());
                    t.setFinalizadaEn(null);
                    t.setError(null);
                    anunciar(t);
                    return t;
                });
    }

    @Transactional
    public void completar(long id, ManejadorTarea.Resultado resultado) {
        repositorio.findById(id).ifPresent(t -> {
            t.setEstado(EstadoTarea.COMPLETADA);
            t.setResumen(resultado.resumen());
            t.setResultado(resultado.contenido());
            t.setFinalizadaEn(LocalDateTime.now());
            anunciar(t);
        });
    }

    /**
     * Registra un intento fallido.
     * @return true si la tarea debe reintentarse (vuelve a PENDIENTE), false si quedó FALLIDA
     */
    @Transactional
    public boolean fallar(long id, Throwable error) {
        return repositorio.findById(id).map(t -> {
            String mensaje = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
            t.setError(mensaje.length() > LARGO_ERROR ? mensaje.substring(0, LARGO_ERROR) : mensaje);
            t.setFinalizadaEn(LocalDateTime.now());
            boolean reintentar = t.getIntentos() < t.getMaxIntentos();
            t.setEstado(reintentar ? EstadoTarea.PENDIENTE : EstadoTarea.FALLIDA);
            anunciar(t);
            return reintentar;
        }).orElse(false);
    }

    @Transactional
    public TareaProceso cancelar(long id) {
        TareaProceso t = obtener(id);
        if (t.getEstado() != EstadoTarea.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden cancelar tareas pendientes (estado actual: " + t.getEstado() + ")");
        }
        t.setEstado(EstadoTarea.CANCELADA);
        t.setFinalizadaEn(LocalDateTime.now());
        cola.quitar(id);
        anunciar(t);
        return t;
    }

    /** Vuelve a encolar una tarea fallida o cancelada con sus intentos reiniciados. */
    @Transactional
    public TareaProceso reintentar(long id) {
        TareaProceso t = obtener(id);
        if (t.getEstado() != EstadoTarea.FALLIDA && t.getEstado() != EstadoTarea.CANCELADA) {
            throw new ReglaNegocioException("Solo se pueden reintentar tareas fallidas o canceladas");
        }
        t.setEstado(EstadoTarea.PENDIENTE);
        t.setIntentos(0);
        t.setError(null);
        cola.encolar(t.getId(), t.getPrioridad());
        anunciar(t);
        return t;
    }

    @Transactional(readOnly = true)
    public TareaProceso obtener(long id) {
        return repositorio.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Tarea", id));
    }

    @Transactional(readOnly = true)
    public List<TareaProceso> recientes() {
        return repositorio.findTop50ByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Map<EstadoTarea, Long> conteoPorEstado() {
        Map<EstadoTarea, Long> conteo = new EnumMap<>(EstadoTarea.class);
        for (EstadoTarea e : EstadoTarea.values()) conteo.put(e, repositorio.countByEstado(e));
        return conteo;
    }

    /**
     * Al arrancar: las tareas que quedaron EN_PROCESO por un reinicio vuelven a PENDIENTE
     * y todas las pendientes regresan a la cola.
     */
    @Transactional
    public int recuperarPendientes() {
        List<TareaProceso> abiertas = repositorio.findByEstadoInOrderByPrioridadDescIdAsc(
                List.of(EstadoTarea.PENDIENTE, EstadoTarea.EN_PROCESO));
        abiertas.forEach(t -> {
            t.setEstado(EstadoTarea.PENDIENTE);
            cola.encolar(t.getId(), t.getPrioridad());
        });
        return abiertas.size();
    }
}
