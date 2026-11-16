package com.example.cobro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cálculo de intereses de microcrédito con interés simple (flat), habitual en préstamos "gota a gota":
 * <pre>
 *   interés     = capital × tasa / 100
 *   montoTotal  = capital + interés
 *   valorCuota  = montoTotal / númeroCuotas   (la última cuota absorbe el redondeo)
 * </pre>
 */
public final class CalculadoraIntereses {

    public static final double TASA_POR_DEFECTO = 20.0;

    private CalculadoraIntereses() {}

    public static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calcularInteres(double capital, double tasaPorcentaje) {
        return redondear(capital * tasaPorcentaje / 100.0);
    }

    public static double calcularMontoTotal(double capital, double tasaPorcentaje) {
        return redondear(capital + calcularInteres(capital, tasaPorcentaje));
    }

    /** Valor de la cuota regular (todas menos, posiblemente, la última). */
    public static double calcularValorCuota(double montoTotal, int numeroCuotas) {
        return redondear(montoTotal / numeroCuotas);
    }

    /**
     * Reparte el monto total en {@code numeroCuotas} cuotas redondeadas a centavos,
     * garantizando que la suma sea exactamente igual al monto total.
     */
    public static List<Double> distribuirCuotas(double montoTotal, int numeroCuotas) {
        if (numeroCuotas <= 0) return Collections.emptyList();
        double cuota = calcularValorCuota(montoTotal, numeroCuotas);
        List<Double> cuotas = new ArrayList<>(Collections.nCopies(numeroCuotas, cuota));
        double ultima = redondear(montoTotal - cuota * (numeroCuotas - 1));
        cuotas.set(numeroCuotas - 1, ultima);
        return cuotas;
    }
}
