package com.example.cobro.service.plan;

import com.example.cobro.model.TipoLiquidacion;
import java.time.LocalDate;

/** Cobro diario: una cuota por día; más de 3 días sin pagar se considera atraso. */
public class PlanDiario extends PlanLiquidacion {

    @Override
    public TipoLiquidacion getTipo() {
        return TipoLiquidacion.DIARIO;
    }

    @Override
    public int cuotasPorDefecto() {
        return 40;
    }

    @Override
    public LocalDate fechaCuota(LocalDate inicio, int numero) {
        return inicio.plusDays(numero);
    }

    @Override
    public int diasParaAtraso() {
        return 4;
    }
}
