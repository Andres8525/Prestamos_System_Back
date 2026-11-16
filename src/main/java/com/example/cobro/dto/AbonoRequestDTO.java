package com.example.cobro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AbonoRequestDTO {

    @NotNull(message = "El préstamo es obligatorio")
    private Long prestamoId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto del abono debe ser mayor a cero")
    private Double monto;

    /** Cobrador que recibe el dinero. Si es null se usa el cobrador del préstamo. */
    private Long cobradorId;

    @Size(max = 255, message = "La observación no puede superar 255 caracteres")
    private String observacion;

    public Long getPrestamoId() { return prestamoId; }
    public void setPrestamoId(Long prestamoId) { this.prestamoId = prestamoId; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public Long getCobradorId() { return cobradorId; }
    public void setCobradorId(Long cobradorId) { this.cobradorId = cobradorId; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
