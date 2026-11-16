package com.example.cobro.worker.manejadores;

import com.example.cobro.ia.CreditScoringService;
import com.example.cobro.ia.ResumenModeloDTO;
import com.example.cobro.model.EntrenamientoModelo;
import com.example.cobro.worker.ManejadorTarea;
import com.example.cobro.worker.TipoTarea;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Reentrena el modelo de scoring en segundo plano (el entrenamiento con validación cruzada tarda unos segundos). */
@Component
public class ReentrenarModeloWorker implements ManejadorTarea {

    private final CreditScoringService scoringService;

    public ReentrenarModeloWorker(CreditScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @Override
    public TipoTarea tipo() {
        return TipoTarea.REENTRENAR_MODELO;
    }

    @Override
    public Resultado ejecutar() {
        ResumenModeloDTO r = scoringService.entrenar(EntrenamientoModelo.Origen.MANUAL);
        return Resultado.de(String.format(Locale.ROOT, "Modelo elegido: %s (AUC prueba %.3f)",
                r.modelo().algoritmo(), r.evaluacionPrueba().auc()));
    }
}
