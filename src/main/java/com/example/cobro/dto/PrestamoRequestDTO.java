package com.example.cobro.dto;

import com.example.cobro.model.TipoLiquidacion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PrestamoRequestDTO {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.0", message = "El monto debe ser mayor a cero")
    @DecimalMax(value = "100000000.0", message = "El monto excede el máximo permitido")
    private Double monto;

    @NotNull(message = "El tipo de liquidación es obligatorio")
    private TipoLiquidacion tipoLiquidacion;

    /** Tasa de interés simple en porcentaje. Si es null se usa la tasa por defecto (20%). */
    @DecimalMin(value = "0.0", message = "La tasa de interés no puede ser negativa")
    @DecimalMax(value = "100.0", message = "La tasa de interés no puede superar el 100%")
    private Double tasaInteres;

    /** Número de cuotas. Si es null se usa el valor por defecto del plan (40 diarias / 4 semanales). */
    @Min(value = 1, message = "Debe haber al menos una cuota")
    @Max(value = 365, message = "El número de cuotas no puede superar 365")
    private Integer numeroCuotas;

    public PrestamoRequestDTO() {}

    public PrestamoRequestDTO(Long clienteId, Double monto, TipoLiquidacion tipoLiquidacion) {
        this.clienteId = clienteId;
        this.monto = monto;
        this.tipoLiquidacion = tipoLiquidacion;
    }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public Double getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(Double tasaInteres) { this.tasaInteres = tasaInteres; }

    public Integer getNumeroCuotas() { return numeroCuotas; }
    public void setNumeroCuotas(Integer numeroCuotas) { this.numeroCuotas = numeroCuotas; }

    public TipoLiquidacion getTipoLiquidacion() { return tipoLiquidacion; }
    public void setTipoLiquidacion(TipoLiquidacion tipoLiquidacion) {
        this.tipoLiquidacion = tipoLiquidacion;
    }
}
