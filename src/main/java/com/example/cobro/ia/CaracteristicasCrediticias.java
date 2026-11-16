package com.example.cobro.ia;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vector de características que describe el comportamiento de pago de un cliente.
 * Es la entrada del modelo de scoring de riesgo.
 *
 * @param promedioDiasAtraso     días promedio de atraso por cuota vencida (tope 30)
 * @param maxDiasAtraso          mayor atraso observado en una cuota (tope 60)
 * @param tasaCuotasAtrasadas    proporción de cuotas vencidas pagadas o adeudadas fuera del plazo de gracia
 * @param tasaAbonosIncompletos  proporción de abonos inferiores a la cuota esperada
 * @param frecuenciaPago         abonos realizados por cuota vencida (1 = paga cada cuota; tope 1.5)
 * @param tasaSaldoVencido       proporción del monto vencido que sigue sin pagarse
 * @param prestamosPagados       préstamos anteriores cancelados en su totalidad (tope 10)
 * @param cuotasEvaluadas        cuotas vencidas analizadas (no es entrada del modelo; mide la evidencia)
 */
public record CaracteristicasCrediticias(
        double promedioDiasAtraso,
        double maxDiasAtraso,
        double tasaCuotasAtrasadas,
        double tasaAbonosIncompletos,
        double frecuenciaPago,
        double tasaSaldoVencido,
        double prestamosPagados,
        int cuotasEvaluadas
) {

    public static final String[] NOMBRES = {
            "promedioDiasAtraso",
            "maxDiasAtraso",
            "tasaCuotasAtrasadas",
            "tasaAbonosIncompletos",
            "frecuenciaPago",
            "tasaSaldoVencido",
            "prestamosPagados"
    };

    public static final String[] DESCRIPCIONES = {
            "Días promedio de atraso por cuota",
            "Máximo atraso registrado (días)",
            "Proporción de cuotas pagadas fuera de plazo",
            "Frecuencia de abonos incompletos",
            "Regularidad de los abonos",
            "Proporción del saldo vencido sin pagar",
            "Préstamos anteriores cancelados"
    };

    /** Cliente sin cuotas vencidas: no hay evidencia de comportamiento. */
    public boolean sinHistorial() {
        return cuotasEvaluadas == 0;
    }

    public double[] comoVector() {
        return new double[]{
                promedioDiasAtraso, maxDiasAtraso, tasaCuotasAtrasadas, tasaAbonosIncompletos,
                frecuenciaPago, tasaSaldoVencido, prestamosPagados
        };
    }

    public Map<String, Double> comoMapa() {
        double[] v = comoVector();
        Map<String, Double> mapa = new LinkedHashMap<>();
        for (int i = 0; i < NOMBRES.length; i++) {
            mapa.put(NOMBRES[i], Math.round(v[i] * 1000.0) / 1000.0);
        }
        return mapa;
    }
}
