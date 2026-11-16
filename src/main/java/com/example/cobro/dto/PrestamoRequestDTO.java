package com.example.cobro.dto;

import com.example.cobro.model.TipoLiquidacion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

    public TipoLiquidacion getTipoLiquidacion() { return tipoLiquidacion; }
    public void setTipoLiquidacion(TipoLiquidacion tipoLiquidacion) {
        this.tipoLiquidacion = tipoLiquidacion;
    }
}
