package com.example.cobro.service;

import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.TipoLiquidacion;
import com.example.cobro.service.plan.PlanLiquidacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraInteresesTest {

    @Test
    void calculaInteresSimpleYMontoTotal() {
        assertEquals(200.0, CalculadoraIntereses.calcularInteres(1000, 20));
        assertEquals(1200.0, CalculadoraIntereses.calcularMontoTotal(1000, 20));
        assertEquals(30.0, CalculadoraIntereses.calcularValorCuota(1200, 40));
    }

    @Test
    void distribuirCuotasSumaExactamenteElTotal() {
        List<Double> cuotas = CalculadoraIntereses.distribuirCuotas(1000, 3);
        assertEquals(3, cuotas.size());
        assertEquals(333.33, cuotas.get(0));
        assertEquals(333.34, cuotas.get(2));
        assertEquals(1000.0, cuotas.stream().mapToDouble(Double::doubleValue).sum(), 0.001);
    }

    @Test
    void planDiarioYSemanalUsanSuPropiaPeriodicidadYTolerancia() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        PlanLiquidacion diario = PlanLiquidacion.de(TipoLiquidacion.DIARIO);
        PlanLiquidacion semanal = PlanLiquidacion.de(TipoLiquidacion.SEMANAL);

        assertEquals(LocalDate.of(2026, 1, 3), diario.fechaCuota(inicio, 2));
        assertEquals(LocalDate.of(2026, 1, 15), semanal.fechaCuota(inicio, 2));
        assertEquals(40, diario.cuotasPorDefecto());
        assertEquals(4, semanal.cuotasPorDefecto());

        assertEquals(EstadoPago.AL_DIA, diario.clasificar(0));
        assertEquals(EstadoPago.PENDIENTE, diario.clasificar(3));
        assertEquals(EstadoPago.ATRASADO, diario.clasificar(4));
        assertEquals(EstadoPago.PENDIENTE, semanal.clasificar(6));
        assertEquals(EstadoPago.ATRASADO, semanal.clasificar(7));
    }
}
