package com.example.cobro.ia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Métricas de evaluación de un clasificador binario sobre un conjunto de muestras:
 * matriz de confusión, exactitud, precisión, sensibilidad (recall), especificidad, F1,
 * AUC-ROC, puntos de la curva ROC y Brier score (error cuadrático de las probabilidades).
 */
public final class MetricasClasificacion {

    public static final double UMBRAL = 0.5;

    private MetricasClasificacion() {}

    /** Matriz de confusión con umbral 0.5 (positivo = incumplimiento). */
    public record MatrizConfusion(int verdaderosPositivos, int falsosPositivos,
                                  int verdaderosNegativos, int falsosNegativos) {}

    /** Punto de la curva ROC: tasa de falsos positivos vs. tasa de verdaderos positivos. */
    public record PuntoRoc(double umbral, double fpr, double tpr) {}

    public record Resultado(
            int muestras,
            MatrizConfusion matriz,
            double exactitud,
            double precision,
            double sensibilidad,
            double especificidad,
            double f1,
            double auc,
            double brier,
            List<PuntoRoc> curvaRoc
    ) {}

    public static Resultado evaluar(ModeloClasificacion modelo, List<Muestra> muestras) {
        int n = muestras.size();
        double[] p = new double[n];
        for (int i = 0; i < n; i++) p[i] = modelo.predecir(muestras.get(i).x());

        int tp = 0, fp = 0, tn = 0, fn = 0;
        double brier = 0;
        for (int i = 0; i < n; i++) {
            int y = muestras.get(i).y();
            boolean predicePositivo = p[i] >= UMBRAL;
            if (predicePositivo && y == 1) tp++;
            else if (predicePositivo) fp++;
            else if (y == 0) tn++;
            else fn++;
            brier += Math.pow(p[i] - y, 2);
        }

        double exactitud = n == 0 ? 0 : (double) (tp + tn) / n;
        double precision = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        double sensibilidad = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        double especificidad = tn + fp == 0 ? 0 : (double) tn / (tn + fp);
        double f1 = precision + sensibilidad == 0 ? 0 : 2 * precision * sensibilidad / (precision + sensibilidad);

        return new Resultado(n, new MatrizConfusion(tp, fp, tn, fn),
                r(exactitud), r(precision), r(sensibilidad), r(especificidad), r(f1),
                r(auc(muestras, p)), r(n == 0 ? 0 : brier / n), curvaRoc(muestras, p));
    }

    /**
     * AUC por el estadístico de Mann-Whitney: probabilidad de que un moroso elegido al azar
     * reciba mayor puntaje de riesgo que un buen pagador (los empates cuentan 0.5).
     */
    static double auc(List<Muestra> muestras, double[] p) {
        int n = muestras.size();
        Integer[] orden = new Integer[n];
        for (int i = 0; i < n; i++) orden[i] = i;
        java.util.Arrays.sort(orden, Comparator.comparingDouble(i -> p[i]));

        double sumaRangos = 0;
        long positivos = 0;
        int i = 0;
        while (i < n) {
            int j = i;
            while (j + 1 < n && p[orden[j + 1]] == p[orden[i]]) j++;
            double rangoMedio = (i + j) / 2.0 + 1;          // rangos 1..n con empates promediados
            for (int k = i; k <= j; k++) {
                if (muestras.get(orden[k]).y() == 1) {
                    sumaRangos += rangoMedio;
                    positivos++;
                }
            }
            i = j + 1;
        }
        long negativos = n - positivos;
        if (positivos == 0 || negativos == 0) return 0.5;
        return (sumaRangos - positivos * (positivos + 1) / 2.0) / ((double) positivos * negativos);
    }

    /** Curva ROC evaluada en 21 umbrales (de 1.0 a 0.0). */
    static List<PuntoRoc> curvaRoc(List<Muestra> muestras, double[] p) {
        long positivos = muestras.stream().filter(m -> m.y() == 1).count();
        long negativos = muestras.size() - positivos;
        List<PuntoRoc> puntos = new ArrayList<>();
        for (int k = 20; k >= 0; k--) {
            double umbral = k / 20.0;
            int tp = 0, fp = 0;
            for (int i = 0; i < p.length; i++) {
                if (p[i] >= umbral) {
                    if (muestras.get(i).y() == 1) tp++;
                    else fp++;
                }
            }
            puntos.add(new PuntoRoc(umbral,
                    negativos == 0 ? 0 : r((double) fp / negativos),
                    positivos == 0 ? 0 : r((double) tp / positivos)));
        }
        return puntos;
    }

    private static double r(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
