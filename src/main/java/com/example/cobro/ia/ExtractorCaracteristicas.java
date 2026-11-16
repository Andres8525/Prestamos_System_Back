package com.example.cobro.ia;

import com.example.cobro.model.Abono;
import com.example.cobro.model.Pago;
import com.example.cobro.service.PagoService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Convierte el historial de cuotas y abonos de un cliente en {@link CaracteristicasCrediticias}.
 * El cálculo se hace "a una fecha de corte": todo lo ocurrido después se ignora,
 * lo que permite construir muestras de entrenamiento sin fuga de información futura.
 */
public final class ExtractorCaracteristicas {

    private static final double TOPE_PROMEDIO = 30;
    private static final double TOPE_MAXIMO = 60;
    private static final double TOPE_FRECUENCIA = 1.5;
    private static final double TOPE_PRESTAMOS = 10;

    private ExtractorCaracteristicas() {}

    public static CaracteristicasCrediticias extraer(List<Pago> cuotas, List<Abono> abonos,
                                                     int prestamosPagados, LocalDate corte) {
        double sumaAtraso = 0;
        double maxAtraso = 0;
        int atrasadas = 0;
        int vencidas = 0;
        double montoVencido = 0;
        double saldoVencido = 0;

        for (Pago cuota : cuotas) {
            if (cuota.getFechaProgramada().isAfter(corte)) continue;
            vencidas++;
            boolean pagadaAlCorte = cuota.getFechaPago() != null && !cuota.getFechaPago().isAfter(corte);
            LocalDate fin = pagadaAlCorte ? cuota.getFechaPago() : corte;
            long atraso = Math.max(0, ChronoUnit.DAYS.between(cuota.getFechaProgramada(), fin));

            sumaAtraso += Math.min(atraso, TOPE_PROMEDIO);
            maxAtraso = Math.max(maxAtraso, Math.min(atraso, TOPE_MAXIMO));
            if (atraso > PagoService.DIAS_GRACIA) atrasadas++;

            montoVencido += cuota.getMonto();
            if (!pagadaAlCorte) {
                // Saldo aproximado al corte: si hoy está pagada pero no lo estaba al corte, se cuenta completa.
                saldoVencido += cuota.getFechaPago() == null ? cuota.saldo() : cuota.getMonto();
            }
        }

        List<Abono> abonosAlCorte = abonos.stream().filter(a -> !a.getFecha().isAfter(corte)).toList();
        long incompletos = abonosAlCorte.stream().filter(Abono::esIncompleto).count();

        double promedio = vencidas == 0 ? 0 : sumaAtraso / vencidas;
        double tasaAtrasadas = vencidas == 0 ? 0 : (double) atrasadas / vencidas;
        double tasaIncompletos = abonosAlCorte.isEmpty() ? 0 : (double) incompletos / abonosAlCorte.size();
        double frecuencia = vencidas == 0 ? 1.0 : Math.min(TOPE_FRECUENCIA, (double) abonosAlCorte.size() / vencidas);
        double tasaSaldo = montoVencido == 0 ? 0 : Math.min(1.0, saldoVencido / montoVencido);

        return new CaracteristicasCrediticias(
                promedio, maxAtraso, tasaAtrasadas, tasaIncompletos, frecuencia, tasaSaldo,
                Math.min(prestamosPagados, TOPE_PRESTAMOS), vencidas);
    }
}
