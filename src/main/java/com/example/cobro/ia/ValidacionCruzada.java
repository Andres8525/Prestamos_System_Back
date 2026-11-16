package com.example.cobro.ia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Herramientas para evaluar modelos sin sobreajuste:
 * división estratificada entrenamiento/prueba y validación cruzada k-fold estratificada.
 * "Estratificada" significa que cada parte conserva la misma proporción de morosos que el total.
 */
public final class ValidacionCruzada {

    private ValidacionCruzada() {}

    public record Division(List<Muestra> entrenamiento, List<Muestra> prueba) {}

    /** Resultado de la validación cruzada: media y desviación de las métricas en los k pliegues. */
    public record Resultado(int pliegues, double aucMedia, double aucDesviacion,
                            double exactitudMedia, double f1Media) {}

    /** Separa una fracción para prueba manteniendo la proporción de clases (semilla fija = reproducible). */
    public static Division dividir(List<Muestra> muestras, double fraccionPrueba, long semilla) {
        List<Muestra> entrenamiento = new ArrayList<>();
        List<Muestra> prueba = new ArrayList<>();
        for (List<Muestra> clase : porClase(muestras, semilla)) {
            int corte = (int) Math.round(clase.size() * fraccionPrueba);
            prueba.addAll(clase.subList(0, corte));
            entrenamiento.addAll(clase.subList(corte, clase.size()));
        }
        return new Division(entrenamiento, prueba);
    }

    /** Validación cruzada k-fold estratificada: entrena k veces dejando fuera un pliegue distinto. */
    public static Resultado evaluar(Supplier<ModeloClasificacion> fabrica, List<Muestra> muestras,
                                    int k, long semilla) {
        List<List<Muestra>> pliegues = new ArrayList<>();
        for (int i = 0; i < k; i++) pliegues.add(new ArrayList<>());
        for (List<Muestra> clase : porClase(muestras, semilla)) {
            for (int i = 0; i < clase.size(); i++) pliegues.get(i % k).add(clase.get(i));
        }

        double[] aucs = new double[k];
        double sumaExactitud = 0, sumaF1 = 0;
        for (int i = 0; i < k; i++) {
            List<Muestra> entrenamiento = new ArrayList<>();
            for (int j = 0; j < k; j++) if (j != i) entrenamiento.addAll(pliegues.get(j));
            ModeloClasificacion modelo = fabrica.get();
            modelo.entrenar(entrenamiento);
            MetricasClasificacion.Resultado r = MetricasClasificacion.evaluar(modelo, pliegues.get(i));
            aucs[i] = r.auc();
            sumaExactitud += r.exactitud();
            sumaF1 += r.f1();
        }
        double media = 0;
        for (double a : aucs) media += a;
        media /= k;
        double varianza = 0;
        for (double a : aucs) varianza += Math.pow(a - media, 2);
        return new Resultado(k, r(media), r(Math.sqrt(varianza / k)), r(sumaExactitud / k), r(sumaF1 / k));
    }

    /** Agrupa las muestras por clase (0 y 1) y las baraja con la semilla indicada. */
    private static List<List<Muestra>> porClase(List<Muestra> muestras, long semilla) {
        List<Muestra> negativos = new ArrayList<>();
        List<Muestra> positivos = new ArrayList<>();
        for (Muestra m : muestras) (m.y() == 1 ? positivos : negativos).add(m);
        Random rnd = new Random(semilla);
        Collections.shuffle(negativos, rnd);
        Collections.shuffle(positivos, rnd);
        return List.of(negativos, positivos);
    }

    private static double r(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
