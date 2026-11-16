package com.example.cobro.ia;

import java.util.List;

/**
 * Reporte completo del último entrenamiento: modelo seleccionado, su evaluación en datos no vistos,
 * la validación cruzada y la comparación entre todos los algoritmos candidatos.
 */
public record ResumenModeloDTO(
        ScoringDTO.ModeloInfo modelo,
        int muestrasEntrenamiento,
        int muestrasPrueba,
        MetricasClasificacion.Resultado evaluacionPrueba,
        ValidacionCruzada.Resultado validacionCruzada,
        List<Comparacion> comparacion
) {
    /** Desempeño de un algoritmo candidato. */
    public record Comparacion(
            String algoritmo,
            double aucValidacion,
            double aucDesviacion,
            double aucPrueba,
            double exactitudPrueba,
            double f1Prueba,
            boolean seleccionado
    ) {}
}
