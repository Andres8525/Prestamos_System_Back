package com.example.cobro.dto;

import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Prestamo;
import com.example.cobro.model.TipoLiquidacion;
import java.time.LocalDate;

public class PrestamoResponseDTO {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long cobradorId;
    private String cobradorNombre;
    private Double monto;
    private Double tasaInteres;
    private Double montoTotal;
    private Double interes;
    private TipoLiquidacion tipoLiquidacion;
    private Integer numeroCuotas;
    private Double valorCuota;
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private EstadoPago estado;

    public PrestamoResponseDTO() {}

    public static PrestamoResponseDTO fromEntity(Prestamo prestamo) {
        PrestamoResponseDTO dto = new PrestamoResponseDTO();
        dto.setId(prestamo.getId());
        dto.setClienteId(prestamo.getClienteId());
        dto.setClienteNombre(prestamo.getCliente() != null ? prestamo.getCliente().getNombre() : null);
        if (prestamo.getCobrador() != null) {
            dto.setCobradorId(prestamo.getCobrador().getId());
            dto.setCobradorNombre(prestamo.getCobrador().getNombre());
        }
        dto.setMonto(prestamo.getMonto());
        dto.setTasaInteres(prestamo.getTasaInteres());
        dto.setMontoTotal(prestamo.getMontoTotal());
        dto.setInteres(prestamo.getMontoTotal() != null && prestamo.getMonto() != null
                ? prestamo.getMontoTotal() - prestamo.getMonto() : null);
        dto.setTipoLiquidacion(prestamo.getTipoLiquidacion());
        dto.setNumeroCuotas(prestamo.getNumeroCuotas());
        dto.setValorCuota(prestamo.getValorCuota());
        dto.setFechaInicio(prestamo.getFechaInicio());
        dto.setFechaVencimiento(prestamo.getFechaVencimiento());
        dto.setEstado(prestamo.getEstado());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public Long getCobradorId() { return cobradorId; }
    public void setCobradorId(Long cobradorId) { this.cobradorId = cobradorId; }

    public String getCobradorNombre() { return cobradorNombre; }
    public void setCobradorNombre(String cobradorNombre) { this.cobradorNombre = cobradorNombre; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public Double getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(Double tasaInteres) { this.tasaInteres = tasaInteres; }

    public Double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(Double montoTotal) { this.montoTotal = montoTotal; }

    public Double getInteres() { return interes; }
    public void setInteres(Double interes) { this.interes = interes; }

    public Integer getNumeroCuotas() { return numeroCuotas; }
    public void setNumeroCuotas(Integer numeroCuotas) { this.numeroCuotas = numeroCuotas; }

    public Double getValorCuota() { return valorCuota; }
    public void setValorCuota(Double valorCuota) { this.valorCuota = valorCuota; }

    public TipoLiquidacion getTipoLiquidacion() { return tipoLiquidacion; }
    public void setTipoLiquidacion(TipoLiquidacion tipoLiquidacion) {
        this.tipoLiquidacion = tipoLiquidacion;
    }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }
}
