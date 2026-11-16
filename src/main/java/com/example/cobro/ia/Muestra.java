package com.example.cobro.ia;

/**
 * Ejemplo de entrenamiento: vector de características, etiqueta (1 = incumplió, 0 = pagó) y peso.
 * El peso permite que los casos reales de la cartera cuenten más que los de referencia.
 */
public record Muestra(double[] x, int y, double peso) {}
