package com.example.cobro.service.plan;

import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.TipoLiquidacion;
import java.time.LocalDate;

/**
 * Estrategia (patrón Strategy) que define cómo se reparte un préstamo en el tiempo.
 * Cada subclase decide la periodicidad de las cuotas y la tolerancia de atraso.
 */
public abstract class PlanLiquidacion {

    /** Tipo de liquidación que implementa la estrategia. */
    public abstract TipoLiquidacion getTipo();

    /** Número de cuotas que se usa cuando el préstamo no especifica uno. */
    public abstract int cuotasPorDefecto();

    /** Fecha en la que vence la cuota número {@code numero} (1..n). */
    public abstract LocalDate fechaCuota(LocalDate inicio, int numero);

    /** Días de atraso a partir de los cuales el préstamo pasa a ATRASADO. */
    public abstract int diasParaAtraso();

    /** Fecha de vencimiento del préstamo completo. */
    public LocalDate fechaVencimiento(LocalDate inicio, int numeroCuotas) {
        return fechaCuota(inicio, numeroCuotas);
    }

    /** Clasifica el estado del préstamo según los días de atraso de la cuota impaga más antigua. */
    public EstadoPago clasificar(long diasAtraso) {
        if (diasAtraso <= 0) return EstadoPago.AL_DIA;
        if (diasAtraso < diasParaAtraso()) return EstadoPago.PENDIENTE;
        return EstadoPago.ATRASADO;
    }

    public static PlanLiquidacion de(TipoLiquidacion tipo) {
        return switch (tipo) {
            case DIARIO -> new PlanDiario();
            case SEMANAL -> new PlanSemanal();
        };
    }
}
