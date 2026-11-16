package com.example.cobro.service.plan;

import com.example.cobro.model.TipoLiquidacion;
import java.time.LocalDate;

/** Cobro semanal: una cuota cada 7 días; una semana completa sin pagar se considera atraso. */
public class PlanSemanal extends PlanLiquidacion {

    @Override
    public TipoLiquidacion getTipo() {
        return TipoLiquidacion.SEMANAL;
    }

    @Override
    public int cuotasPorDefecto() {
        return 4;
    }

    @Override
    public LocalDate fechaCuota(LocalDate inicio, int numero) {
        return inicio.plusWeeks(numero);
    }

    @Override
    public int diasParaAtraso() {
        return 7;
    }
}
