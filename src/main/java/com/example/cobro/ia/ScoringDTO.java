package com.example.cobro.ia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Resultado del scoring de riesgo crediticio de un cliente. */
public record ScoringDTO(
        Long clienteId,
        String clienteNombre,
        double probabilidadDefault,
        int puntaje,
        NivelRiesgo nivelRiesgo,
        String recomendacion,
        String mensaje,
        boolean sinHistorial,
        int prestamosAnteriores,
        int cuotasEvaluadas,
        Map<String, Double> caracteristicas,
        List<Factor> factores,
        ModeloInfo modelo
) {
    /** Variable que más empuja el riesgo hacia arriba (impacto &gt; 0) o hacia abajo (impacto &lt; 0). */
    public record Factor(String variable, String descripcion, double valor, double impacto) {}

    /** Metadatos del modelo que generó la predicción. */
    public record ModeloInfo(
            String algoritmo,
            int muestrasReales,
            int muestrasSinteticas,
            double exactitud,
            double auc,
            LocalDateTime entrenadoEn,
            /** Pesos de la regresión logística (null si el modelo elegido es otro algoritmo). */
            Map<String, Double> pesos,
            /** Importancia relativa de cada variable en el modelo (suma 1). */
            Map<String, Double> importancia
    ) {}
}
