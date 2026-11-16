package com.example.cobro.worker;

import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.TareaProceso;
import com.example.cobro.security.AuthFilter;
import com.example.cobro.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** API de la cola de tareas en segundo plano (workers). */
@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    private final TareaService tareas;
    private final WorkerPool pool;
    private final AuthService authService;

    public WorkerController(TareaService tareas, WorkerPool pool, AuthService authService) {
        this.tareas = tareas;
        this.pool = pool;
        this.authService = authService;
    }

    public record TareaDTO(Long id, TipoTarea tipo, String nombre, EstadoTarea estado, int prioridad,
                           int intentos, int maxIntentos, String worker, String solicitadaPor, String resumen,
                           String error, boolean tieneResultado, LocalDateTime creadaEn,
                           LocalDateTime iniciadaEn, LocalDateTime finalizadaEn, Long duracionMs) {
        static TareaDTO de(TareaProceso t) {
            return new TareaDTO(t.getId(), t.getTipo(), t.getTipo().getNombre(), t.getEstado(), t.getPrioridad(),
                    t.getIntentos(), t.getMaxIntentos(), t.getWorker(), t.getSolicitadaPor(), t.getResumen(),
                    t.getError(), t.getResultado() != null, t.getCreadaEn(), t.getIniciadaEn(),
                    t.getFinalizadaEn(), t.duracionMs());
        }
    }

    public record TipoDTO(TipoTarea tipo, String nombre, String descripcion, boolean disponible) {}

    public record EstadoPoolDTO(int hilos, int ocupados, int enCola, long completadas, long fallidas,
                                Map<EstadoTarea, Long> tareasPorEstado, List<TipoDTO> tipos) {}

    public record NuevaTarea(@NotNull TipoTarea tipo, @Min(0) @Max(10) Integer prioridad) {}

    @GetMapping("/estado")
    public EstadoPoolDTO estado() {
        List<TipoDTO> tipos = Arrays.stream(TipoTarea.values())
                .map(t -> new TipoDTO(t, t.getNombre(), t.getDescripcion(), pool.soporta(t)))
                .toList();
        return new EstadoPoolDTO(pool.getNumeroHilos(), pool.getOcupados(), pool.getEnCola(),
                pool.getCompletadas(), pool.getFallidas(), tareas.conteoPorEstado(), tipos);
    }

    @GetMapping("/tareas")
    public List<TareaDTO> listar() {
        return tareas.recientes().stream().map(TareaDTO::de).toList();
    }

    @GetMapping("/tareas/{id}")
    public TareaDTO obtener(@PathVariable long id) {
        return TareaDTO.de(tareas.obtener(id));
    }

    @PostMapping("/tareas")
    public ResponseEntity<TareaDTO> encolar(@Valid @RequestBody NuevaTarea nueva, HttpServletRequest request) {
        if (!pool.soporta(nueva.tipo())) {
            throw new ReglaNegocioException("No hay un worker disponible para " + nueva.tipo().getNombre());
        }
        Long usuarioId = (Long) request.getAttribute(AuthFilter.ATRIBUTO_USUARIO_ID);
        String solicitante = usuarioId != null ? authService.perfil(usuarioId).email() : null;
        TareaProceso t = tareas.encolar(nueva.tipo(), nueva.prioridad() != null ? nueva.prioridad() : 5, solicitante);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(TareaDTO.de(t));
    }

    @PostMapping("/tareas/{id}/reintentar")
    public TareaDTO reintentar(@PathVariable long id) {
        return TareaDTO.de(tareas.reintentar(id));
    }

    @DeleteMapping("/tareas/{id}")
    public TareaDTO cancelar(@PathVariable long id) {
        return TareaDTO.de(tareas.cancelar(id));
    }

    /** Descarga el contenido generado por la tarea (CSV del reporte o texto del informe). */
    @GetMapping("/tareas/{id}/resultado")
    public ResponseEntity<String> resultado(@PathVariable long id) {
        TareaProceso t = tareas.obtener(id);
        if (t.getResultado() == null) {
            throw new ReglaNegocioException("La tarea no generó contenido descargable");
        }
        boolean csv = t.getTipo() == TipoTarea.REPORTE_CARTERA;
        String archivo = (csv ? "reporte-cartera-" : "informe-ia-") + id + (csv ? ".csv" : ".md");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                .contentType(new MediaType(csv ? MediaType.valueOf("text/csv") : MediaType.TEXT_PLAIN,
                        java.nio.charset.StandardCharsets.UTF_8))
                .body(t.getResultado());
    }
}
