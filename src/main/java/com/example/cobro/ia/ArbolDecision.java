package com.example.cobro.ia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Árbol de decisión de clasificación (CART) construido con el índice de impureza de Gini.
 *
 * <p>En cada nodo prueba los cortes "variable ≤ umbral" y se queda con el que más reduce la
 * impureza ponderada. Se detiene al llegar a la profundidad máxima, cuando el nodo queda con muy
 * poco peso o cuando ningún corte mejora. La hoja devuelve la proporción (suavizada) de morosos.</p>
 *
 * <p>Explicación de cada predicción (método de Saabas): al bajar por el árbol, el cambio de
 * probabilidad entre un nodo y su hijo se atribuye a la variable que se usó para el corte.</p>
 */
public class ArbolDecision implements ModeloClasificacion {

    private static final int MAX_UMBRALES = 32;

    private final int profundidadMaxima;
    private final double pesoMinimoHoja;

    private Nodo raiz;
    private double[] importanciaAcumulada;

    public ArbolDecision() {
        this(4, 15.0);
    }

    public ArbolDecision(int profundidadMaxima, double pesoMinimoHoja) {
        this.profundidadMaxima = profundidadMaxima;
        this.pesoMinimoHoja = pesoMinimoHoja;
    }

    /** Nodo del árbol: interno (con corte) u hoja (variable = -1). */
    private static final class Nodo {
        int variable = -1;
        double umbral;
        double probabilidad;
        Nodo izquierdo;
        Nodo derecho;

        boolean esHoja() {
            return variable < 0;
        }
    }

    @Override
    public String nombre() {
        return "Árbol de decisión (CART)";
    }

    @Override
    public void entrenar(List<Muestra> muestras) {
        if (muestras.isEmpty()) throw new IllegalArgumentException("No hay muestras para entrenar");
        importanciaAcumulada = new double[muestras.get(0).x().length];
        raiz = construir(new ArrayList<>(muestras), 0);
    }

    private Nodo construir(List<Muestra> muestras, int profundidad) {
        Nodo nodo = new Nodo();
        double peso = pesoTotal(muestras);
        double pesoPositivo = pesoPositivo(muestras);
        // Suavizado de Laplace: evita probabilidades exactas de 0 o 1 en hojas pequeñas.
        nodo.probabilidad = (pesoPositivo + 1) / (peso + 2);

        if (profundidad >= profundidadMaxima || peso < 2 * pesoMinimoHoja
                || pesoPositivo == 0 || pesoPositivo == peso) {
            return nodo;
        }

        double impurezaPadre = gini(pesoPositivo, peso);
        double mejorGanancia = 1e-9;
        int mejorVariable = -1;
        double mejorUmbral = 0;

        int d = muestras.get(0).x().length;
        for (int j = 0; j < d; j++) {
            for (double umbral : umbralesCandidatos(muestras, j)) {
                double pesoIzq = 0, posIzq = 0;
                for (Muestra m : muestras) {
                    if (m.x()[j] <= umbral) {
                        pesoIzq += m.peso();
                        if (m.y() == 1) posIzq += m.peso();
                    }
                }
                double pesoDer = peso - pesoIzq;
                if (pesoIzq < pesoMinimoHoja || pesoDer < pesoMinimoHoja) continue;
                double posDer = pesoPositivo - posIzq;
                double impurezaHijos = (pesoIzq * gini(posIzq, pesoIzq) + pesoDer * gini(posDer, pesoDer)) / peso;
                double ganancia = impurezaPadre - impurezaHijos;
                if (ganancia > mejorGanancia) {
                    mejorGanancia = ganancia;
                    mejorVariable = j;
                    mejorUmbral = umbral;
                }
            }
        }
        if (mejorVariable < 0) return nodo;

        List<Muestra> izquierda = new ArrayList<>();
        List<Muestra> derecha = new ArrayList<>();
        for (Muestra m : muestras) {
            (m.x()[mejorVariable] <= mejorUmbral ? izquierda : derecha).add(m);
        }
        importanciaAcumulada[mejorVariable] += mejorGanancia * peso;
        nodo.variable = mejorVariable;
        nodo.umbral = mejorUmbral;
        nodo.izquierdo = construir(izquierda, profundidad + 1);
        nodo.derecho = construir(derecha, profundidad + 1);
        return nodo;
    }

    /** Puntos medios entre valores distintos; si hay muchos se toman cuantiles para acotar el cálculo. */
    private static double[] umbralesCandidatos(List<Muestra> muestras, int j) {
        TreeSet<Double> valores = new TreeSet<>();
        for (Muestra m : muestras) valores.add(m.x()[j]);
        Double[] v = valores.toArray(new Double[0]);
        if (v.length < 2) return new double[0];
        int pasos = Math.min(MAX_UMBRALES, v.length - 1);
        double[] umbrales = new double[pasos];
        for (int k = 0; k < pasos; k++) {
            int i = (int) ((long) (k + 1) * (v.length - 1) / (pasos + 1));
            umbrales[k] = (v[i] + v[i + 1]) / 2.0;
        }
        return Arrays.stream(umbrales).distinct().toArray();
    }

    private static double gini(double positivos, double total) {
        if (total <= 0) return 0;
        double p = positivos / total;
        return 2 * p * (1 - p);
    }

    private static double pesoTotal(List<Muestra> muestras) {
        return muestras.stream().mapToDouble(Muestra::peso).sum();
    }

    private static double pesoPositivo(List<Muestra> muestras) {
        return muestras.stream().filter(m -> m.y() == 1).mapToDouble(Muestra::peso).sum();
    }

    @Override
    public double predecir(double[] x) {
        verificarEntrenado();
        Nodo n = raiz;
        while (!n.esHoja()) n = x[n.variable] <= n.umbral ? n.izquierdo : n.derecho;
        return n.probabilidad;
    }

    @Override
    public double[] contribuciones(double[] x) {
        verificarEntrenado();
        double[] c = new double[x.length];
        Nodo n = raiz;
        while (!n.esHoja()) {
            Nodo siguiente = x[n.variable] <= n.umbral ? n.izquierdo : n.derecho;
            c[n.variable] += siguiente.probabilidad - n.probabilidad;
            n = siguiente;
        }
        return c;
    }

    /** Importancia de Gini: reducción total de impureza lograda por cada variable, normalizada. */
    @Override
    public double[] importancia() {
        verificarEntrenado();
        return ModeloClasificacion.normalizar(importanciaAcumulada);
    }

    @Override
    public boolean estaEntrenado() {
        return raiz != null;
    }

    /** Número de hojas del árbol (medida de su complejidad). */
    public int contarHojas() {
        verificarEntrenado();
        return contarHojas(raiz);
    }

    private int contarHojas(Nodo n) {
        return n.esHoja() ? 1 : contarHojas(n.izquierdo) + contarHojas(n.derecho);
    }

    private void verificarEntrenado() {
        if (!estaEntrenado()) throw new IllegalStateException("El modelo no ha sido entrenado");
    }
}
