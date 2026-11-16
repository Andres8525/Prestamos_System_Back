package com.example.cobro.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Productor programado: encola trabajos periódicos en lugar de ejecutarlos en el hilo del scheduler,
 * así el scheduler queda libre y los workers aplican reintentos y registro de cada ejecución.
 */
@Component
@ConditionalOnProperty(name = "app.workers.programador.habilitado", havingValue = "true", matchIfMissing = true)
public class ProgramadorTareas {

    private static final Logger log = LoggerFactory.getLogger(ProgramadorTareas.class);
    private static final String SISTEMA = "programador";

    private final TareaService tareas;

    public ProgramadorTareas(TareaService tareas) {
        this.tareas = tareas;
    }

    /** Cada hora (minuto 5): mantiene al día el estado de mora de la cartera. */
    @Scheduled(cron = "${app.workers.mora.cron:0 5 * * * *}")
    public void recalcularMora() {
        tareas.encolar(TipoTarea.RECALCULAR_MORA, 3, SISTEMA);
        log.debug("Encolado el recálculo de mora");
    }
}
