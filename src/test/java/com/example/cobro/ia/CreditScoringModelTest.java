package com.example.cobro.ia;

import com.example.cobro.model.Abono;
import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Pago;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreditScoringModelTest {

    @Test
    void elModeloSeparaBuenosPagadoresDeMorososEnLaCarteraSintetica() {
        List<Muestra> entrenamiento = CreditScoringService.generarMuestrasSinteticas(800, 1L);
        List<Muestra> prueba = CreditScoringService.generarMuestrasSinteticas(300, 99L);

        RegresionLogistica modelo = new RegresionLogistica();
        modelo.entrenar(entrenamiento);

        assertTrue(modelo.auc(prueba) > 0.8, "AUC en datos no vistos debe superar 0.8");
        assertTrue(modelo.exactitud(prueba) > 0.7, "Exactitud en datos no vistos debe superar 0.7");

        double[] buenPagador = {0, 0, 0, 0, 1.0, 0, 4};
        double[] moroso = {20, 45, 0.9, 0.8, 0.3, 0.7, 0};
        assertTrue(modelo.predecir(buenPagador) < NivelRiesgo.UMBRAL_MEDIO);
        assertTrue(modelo.predecir(moroso) >= NivelRiesgo.UMBRAL_ALTO);
    }

    @Test
    void extractorMideAtrasosYAbonosIncompletosAUnaFechaDeCorte() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        List<Pago> cuotas = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Pago p = new Pago();
            p.setNumero(i);
            p.setMonto(10.0);
            p.setFechaProgramada(inicio.plusDays(i));
            p.setEstado(EstadoPago.AL_DIA);
            cuotas.add(p);
        }
        cuotas.get(0).setFechaPago(inicio.plusDays(1));   // a tiempo
        cuotas.get(0).setMontoPagado(10.0);
        cuotas.get(1).setFechaPago(inicio.plusDays(8));   // 6 días tarde
        cuotas.get(1).setMontoPagado(10.0);
        // cuotas 3 y 4 sin pagar

        Abono completo = abono(10.0, 10.0, inicio.plusDays(1));
        Abono incompleto = abono(4.0, 10.0, inicio.plusDays(5));

        CaracteristicasCrediticias c = ExtractorCaracteristicas.extraer(
                cuotas, List.of(completo, incompleto), 1, inicio.plusDays(10));

        assertEquals(4, c.cuotasEvaluadas());
        assertEquals(0.5, c.tasaAbonosIncompletos(), 1e-9);
        assertEquals(0.75, c.tasaCuotasAtrasadas(), 1e-9); // cuota 2 (6d), 3 (7d) y 4 (6d)
        assertEquals(0.5, c.tasaSaldoVencido(), 1e-9);
        assertEquals(7, c.maxDiasAtraso(), 1e-9);
        assertFalse(c.sinHistorial());
    }

    @Test
    void etiquetaIncumplimientoSoloConMasDe30DiasDeMora() {
        LocalDate hoy = LocalDate.of(2026, 6, 1);
        Pago vencida = new Pago();
        vencida.setMonto(10.0);
        vencida.setFechaProgramada(hoy.minusDays(31));
        assertEquals(1, CreditScoringService.etiquetar(List.of(vencida), hoy));

        vencida.setFechaProgramada(hoy.minusDays(5));
        assertNull(CreditScoringService.etiquetar(List.of(vencida), hoy));

        vencida.setFechaPago(hoy.minusDays(2));
        assertEquals(0, CreditScoringService.etiquetar(List.of(vencida), hoy));
    }

    private Abono abono(double monto, double cuotaEsperada, LocalDate fecha) {
        Abono a = new Abono();
        a.setMonto(monto);
        a.setCuotaEsperada(cuotaEsperada);
        a.setFecha(fecha);
        return a;
    }
}
