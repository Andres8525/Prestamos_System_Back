package com.example.cobro.ia;

import com.example.cobro.worker.TareaService;
import com.example.cobro.worker.TipoTarea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea diaria del módulo de Machine Learning. No ejecuta el trabajo en el hilo del scheduler:
 * encola dos tareas para los workers, en orden de prioridad:
 * <ol>
 *   <li>reentrenar el modelo con los préstamos que ya tienen desenlace;</li>
 *   <li>guardar una predicción de seguimiento para cada cliente con crédito activo
 *       (así se ve la evolución de su riesgo).</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(name = "app.ml.reentrenamiento.habilitado", havingValue = "true", matchIfMissing = true)
public class ReentrenamientoProgramado {

    private static final Logger log = LoggerFactory.getLogger(ReentrenamientoProgramado.class);

    private final TareaService tareas;

    public ReentrenamientoProgramado(TareaService tareas) {
        this.tareas = tareas;
    }

    @Scheduled(cron = "${app.ml.reentrenamiento.cron:0 0 3 * * *}")
    public void ejecutar() {
        tareas.encolar(TipoTarea.REENTRENAR_MODELO, 8, "programador");
        tareas.encolar(TipoTarea.SCORING_CARTERA, 7, "programador");
        log.info("Reentrenamiento diario y scoring de cartera encolados para los workers");
    }
}
