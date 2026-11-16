package com.example.cobro.dto;

import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Pago;
import com.example.cobro.model.Prestamo;
import com.example.cobro.model.TipoLiquidacion;
import java.time.LocalDate;
import java.util.List;

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
    private Double probabilidadDefault;
    private String nivelRiesgo;
    private Boolean aprobadoConAlerta;
    private Double totalPagado;
    private Double saldoPendiente;
    private Integer cuotasPagadas;
    private Integer cuotasPendientes;

    public PrestamoResponseDTO() {}

    /** Incluye el resumen de cartera calculado a partir de las cuotas del préstamo. */
    public static PrestamoResponseDTO fromEntity(Prestamo prestamo, List<Pago> cuotas) {
        PrestamoResponseDTO dto = fromEntity(prestamo);
        double pagado = cuotas.stream().mapToDouble(Pago::montoPagadoEfectivo).sum();
        double saldo = cuotas.stream().mapToDouble(Pago::saldo).sum();
        int pagadas = (int) cuotas.stream().filter(Pago::estaPagada).count();
        dto.setTotalPagado(Math.round(pagado * 100.0) / 100.0);
        dto.setSaldoPendiente(Math.round(saldo * 100.0) / 100.0);
        dto.setCuotasPagadas(pagadas);
        dto.setCuotasPendientes(cuotas.size() - pagadas);
        if (dto.getNumeroCuotas() == null) dto.setNumeroCuotas(cuotas.size());
        return dto;
    }

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
        dto.setProbabilidadDefault(prestamo.getProbabilidadDefault());
        dto.setNivelRiesgo(prestamo.getNivelRiesgo());
        dto.setAprobadoConAlerta(prestamo.getAprobadoConAlerta());
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

    public Double getProbabilidadDefault() { return probabilidadDefault; }
    public void setProbabilidadDefault(Double probabilidadDefault) { this.probabilidadDefault = probabilidadDefault; }

    public String getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(String nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }

    public Boolean getAprobadoConAlerta() { return aprobadoConAlerta; }
    public void setAprobadoConAlerta(Boolean aprobadoConAlerta) { this.aprobadoConAlerta = aprobadoConAlerta; }

    public Double getTotalPagado() { return totalPagado; }
    public void setTotalPagado(Double totalPagado) { this.totalPagado = totalPagado; }

    public Double getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(Double saldoPendiente) { this.saldoPendiente = saldoPendiente; }

    public Integer getCuotasPagadas() { return cuotasPagadas; }
    public void setCuotasPagadas(Integer cuotasPagadas) { this.cuotasPagadas = cuotasPagadas; }

    public Integer getCuotasPendientes() { return cuotasPendientes; }
    public void setCuotasPendientes(Integer cuotasPendientes) { this.cuotasPendientes = cuotasPendientes; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }
}
