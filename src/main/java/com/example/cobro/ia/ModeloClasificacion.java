package com.example.cobro.ia;

import java.util.List;

/**
 * Contrato común de los modelos de Machine Learning que estiman la probabilidad de incumplimiento.
 * Cada algoritmo (regresión logística, árbol de decisión, ...) lo implementa a su manera
 * y el servicio de scoring los trata de forma polimórfica para compararlos y elegir el mejor.
 */
public interface ModeloClasificacion {

    /** Nombre legible del algoritmo. */
    String nombre();

    /** Ajusta el modelo a las muestras de entrenamiento. */
    void entrenar(List<Muestra> muestras);

    /** Probabilidad de la clase positiva (incumplimiento) para un vector de características. */
    double predecir(double[] x);

    /**
     * Aporte de cada variable a la predicción de {@code x}: positivo sube el riesgo, negativo lo baja.
     * Sirve para explicar al prestamista por qué el modelo dio ese resultado.
     */
    double[] contribuciones(double[] x);

    /** Importancia global de cada variable en el modelo, normalizada para sumar 1. */
    double[] importancia();

    boolean estaEntrenado();

    /** Proporción de aciertos con umbral 0.5. */
    default double exactitud(List<Muestra> muestras) {
        return MetricasClasificacion.evaluar(this, muestras).exactitud();
    }

    /** Área bajo la curva ROC. */
    default double auc(List<Muestra> muestras) {
        return MetricasClasificacion.evaluar(this, muestras).auc();
    }

    /** Normaliza valores absolutos para que sumen 1 (utilidad para {@link #importancia()}). */
    static double[] normalizar(double[] valores) {
        double total = 0;
        for (double v : valores) total += Math.abs(v);
        double[] r = new double[valores.length];
        for (int i = 0; i < valores.length; i++) r[i] = total == 0 ? 0 : Math.abs(valores[i]) / total;
        return r;
    }
}
