package com.example.cobro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class CierreCajaRequestDTO {

    @NotNull(message = "El cobrador es obligatorio")
    private Long cobradorId;

    /** Día a cerrar; por defecto hoy. */
    private LocalDate fecha;

    /** Base con la que salió el cobrador; por defecto la caja base configurada. */
    @DecimalMin(value = "0.0", message = "La base no puede ser negativa")
    private Double baseInicial;

    @DecimalMin(value = "0.0", message = "Los gastos no pueden ser negativos")
    private Double gastos;

    @NotNull(message = "El efectivo entregado es obligatorio")
    @DecimalMin(value = "0.0", message = "El efectivo entregado no puede ser negativo")
    private Double efectivoEntregado;

    @Size(max = 255, message = "La observación no puede superar 255 caracteres")
    private String observacion;

    public Long getCobradorId() { return cobradorId; }
    public void setCobradorId(Long cobradorId) { this.cobradorId = cobradorId; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getBaseInicial() { return baseInicial; }
    public void setBaseInicial(Double baseInicial) { this.baseInicial = baseInicial; }

    public Double getGastos() { return gastos; }
    public void setGastos(Double gastos) { this.gastos = gastos; }

    public Double getEfectivoEntregado() { return efectivoEntregado; }
    public void setEfectivoEntregado(Double efectivoEntregado) { this.efectivoEntregado = efectivoEntregado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
