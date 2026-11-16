package com.example.cobro.exception;

import com.example.cobro.ia.ScoringDTO;

/**
 * Alerta del modelo de scoring: la renovación solicitada tiene riesgo ALTO de incumplimiento.
 * Se traduce a HTTP 409 e incluye el scoring para que el prestamista decida.
 */
public class RiesgoCreditoException extends RuntimeException {

    private final ScoringDTO scoring;

    public RiesgoCreditoException(ScoringDTO scoring) {
        super(String.format(java.util.Locale.ROOT,
                "Alerta de riesgo: %s tiene %.0f%% de probabilidad de incumplimiento (riesgo %s). "
                        + "Para aprobar la renovación de todos modos, confirme con forzarAprobacion=true.",
                scoring.clienteNombre(), scoring.probabilidadDefault() * 100, scoring.nivelRiesgo()));
        this.scoring = scoring;
    }

    public ScoringDTO getScoring() {
        return scoring;
    }
}
