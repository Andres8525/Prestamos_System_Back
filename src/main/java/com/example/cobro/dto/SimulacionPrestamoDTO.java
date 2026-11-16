package com.example.cobro.dto;

import com.example.cobro.model.TipoLiquidacion;
import java.time.LocalDate;

/** Resultado del cálculo automático de un préstamo antes de emitirlo. */
public record SimulacionPrestamoDTO(
        Double monto,
        Double tasaInteres,
        Double interes,
        Double montoTotal,
        TipoLiquidacion tipoLiquidacion,
        Integer numeroCuotas,
        Double valorCuota,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento
) {}
