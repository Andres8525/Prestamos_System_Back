package com.example.cobro.ia;

import com.example.cobro.model.EntrenamientoModelo;
import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.PrediccionRiesgo;
import com.example.cobro.model.Prestamo;
import com.example.cobro.repository.PrestamoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tarea diaria del módulo de Machine Learning:
 * reentrena el modelo con los préstamos que ya tienen desenlace y guarda una predicción de
 * seguimiento para cada cliente con crédito activo (así se ve la evolución de su riesgo).
 */
@Component
@ConditionalOnProperty(name = "app.ml.reentrenamiento.habilitado", havingValue = "true", matchIfMissing = true)
public class ReentrenamientoProgramado {

    private static final Logger log = LoggerFactory.getLogger(ReentrenamientoProgramado.class);

    private final CreditScoringService scoringService;
    private final HistorialMlService historialMl;
    private final PrestamoRepository prestamoRepository;

    public ReentrenamientoProgramado(CreditScoringService scoringService, HistorialMlService historialMl,
                                     PrestamoRepository prestamoRepository) {
        this.scoringService = scoringService;
        this.historialMl = historialMl;
        this.prestamoRepository = prestamoRepository;
    }

    @Scheduled(cron = "${app.ml.reentrenamiento.cron:0 0 3 * * *}")
    public void ejecutar() {
        try {
            ResumenModeloDTO resumen = scoringService.entrenar(EntrenamientoModelo.Origen.PROGRAMADO);
            Set<Long> clientesActivos = prestamoRepository.findAll().stream()
                    .filter(p -> p.getEstado() != EstadoPago.PAGADO)
                    .map(Prestamo::getClienteId)
                    .collect(Collectors.toSet());
            clientesActivos.forEach(id -> historialMl.registrarPrediccion(
                    scoringService.evaluarCliente(id), PrediccionRiesgo.Origen.SEGUIMIENTO));
            log.info("Reentrenamiento programado: {} (AUC prueba {}), {} clientes en seguimiento",
                    resumen.modelo().algoritmo(), resumen.evaluacionPrueba().auc(), clientesActivos.size());
        } catch (Exception e) {
            log.warn("Falló el reentrenamiento programado: {}", e.getMessage());
        }
    }
}
