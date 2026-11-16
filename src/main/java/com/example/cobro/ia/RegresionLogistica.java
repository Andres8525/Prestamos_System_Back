package com.example.cobro.ia;

import java.util.Arrays;
import java.util.List;

/**
 * Modelo de clasificación binaria por regresión logística, entrenado con descenso de gradiente
 * por lotes y regularización L2. Las entradas se estandarizan (media 0, desviación 1).
 * <pre>
 *   P(default | x) = σ(b + Σ wᵢ·zᵢ),   zᵢ = (xᵢ − μᵢ) / σᵢ,   σ(t) = 1 / (1 + e^(−t))
 * </pre>
 */
public class RegresionLogistica implements ModeloClasificacion {

    private final int iteraciones;
    private final double tasaAprendizaje;
    private final double lambdaL2;

    private double[] pesos;
    private double sesgo;
    private double[] medias;
    private double[] desviaciones;

    public RegresionLogistica() {
        this(3000, 0.1, 0.001);
    }

    public RegresionLogistica(int iteraciones, double tasaAprendizaje, double lambdaL2) {
        this.iteraciones = iteraciones;
        this.tasaAprendizaje = tasaAprendizaje;
        this.lambdaL2 = lambdaL2;
    }

    @Override
    public String nombre() {
        return "Regresión logística";
    }

    @Override
    public void entrenar(List<Muestra> muestras) {
        if (muestras.isEmpty()) throw new IllegalArgumentException("No hay muestras para entrenar");
        int n = muestras.size();
        int d = muestras.get(0).x().length;
        calcularEscalado(muestras, d);

        double[][] z = new double[n][];
        for (int i = 0; i < n; i++) z[i] = estandarizar(muestras.get(i).x());
        double pesoTotal = muestras.stream().mapToDouble(Muestra::peso).sum();

        pesos = new double[d];
        sesgo = 0;
        for (int it = 0; it < iteraciones; it++) {
            double[] grad = new double[d];
            double gradSesgo = 0;
            for (int i = 0; i < n; i++) {
                Muestra m = muestras.get(i);
                double error = (sigmoide(lineal(z[i])) - m.y()) * m.peso();
                for (int j = 0; j < d; j++) grad[j] += error * z[i][j];
                gradSesgo += error;
            }
            for (int j = 0; j < d; j++) {
                pesos[j] -= tasaAprendizaje * (grad[j] / pesoTotal + lambdaL2 * pesos[j]);
            }
            sesgo -= tasaAprendizaje * gradSesgo / pesoTotal;
        }
    }

    /** Probabilidad estimada de la clase positiva (incumplimiento). */
    @Override
    public double predecir(double[] x) {
        verificarEntrenado();
        return sigmoide(lineal(estandarizar(x)));
    }

    /** Contribución de cada variable al logit (wᵢ·zᵢ): positiva aumenta el riesgo. */
    @Override
    public double[] contribuciones(double[] x) {
        verificarEntrenado();
        double[] z = estandarizar(x);
        double[] c = new double[z.length];
        for (int j = 0; j < z.length; j++) c[j] = pesos[j] * z[j];
        return c;
    }

    /** Importancia global: magnitud de cada peso (las variables están estandarizadas), normalizada. */
    @Override
    public double[] importancia() {
        verificarEntrenado();
        return ModeloClasificacion.normalizar(pesos);
    }

    @Override
    public boolean estaEntrenado() {
        return pesos != null;
    }

    public double[] getPesos() {
        return pesos == null ? null : Arrays.copyOf(pesos, pesos.length);
    }

    public double getSesgo() {
        return sesgo;
    }

    private void calcularEscalado(List<Muestra> muestras, int d) {
        medias = new double[d];
        desviaciones = new double[d];
        for (Muestra m : muestras) for (int j = 0; j < d; j++) medias[j] += m.x()[j];
        for (int j = 0; j < d; j++) medias[j] /= muestras.size();
        for (Muestra m : muestras) {
            for (int j = 0; j < d; j++) desviaciones[j] += Math.pow(m.x()[j] - medias[j], 2);
        }
        for (int j = 0; j < d; j++) {
            desviaciones[j] = Math.sqrt(desviaciones[j] / muestras.size());
            if (desviaciones[j] < 1e-9) desviaciones[j] = 1.0;
        }
    }

    private double[] estandarizar(double[] x) {
        double[] z = new double[x.length];
        for (int j = 0; j < x.length; j++) z[j] = (x[j] - medias[j]) / desviaciones[j];
        return z;
    }

    private double lineal(double[] z) {
        double t = sesgo;
        for (int j = 0; j < z.length; j++) t += pesos[j] * z[j];
        return t;
    }

    private static double sigmoide(double t) {
        return 1.0 / (1.0 + Math.exp(-t));
    }

    private void verificarEntrenado() {
        if (!estaEntrenado()) throw new IllegalStateException("El modelo no ha sido entrenado");
    }
}
