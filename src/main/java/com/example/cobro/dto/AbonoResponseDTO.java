package com.example.cobro.dto;

import com.example.cobro.model.Abono;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AbonoResponseDTO {

    private Long id;
    private Long prestamoId;
    private Long clienteId;
    private String clienteNombre;
    private Long cobradorId;
    private String cobradorNombre;
    private Double monto;
    private Double cuotaEsperada;
    private boolean incompleto;
    private LocalDate fecha;
    private LocalDateTime fechaRegistro;
    private String observacion;

    /** Solo en la respuesta de registro: cuotas que quedaron totalmente cubiertas con este abono. */
    private Integer cuotasCubiertas;
    /** Solo en la respuesta de registro: saldo del préstamo tras aplicar el abono. */
    private Double saldoPrestamo;

    public static AbonoResponseDTO fromEntity(Abono a) {
        AbonoResponseDTO dto = new AbonoResponseDTO();
        dto.id = a.getId();
        dto.prestamoId = a.getPrestamo().getId();
        if (a.getPrestamo().getCliente() != null) {
            dto.clienteId = a.getPrestamo().getCliente().getId();
            dto.clienteNombre = a.getPrestamo().getCliente().getNombre();
        }
        if (a.getCobrador() != null) {
            dto.cobradorId = a.getCobrador().getId();
            dto.cobradorNombre = a.getCobrador().getNombre();
        }
        dto.monto = a.getMonto();
        dto.cuotaEsperada = a.getCuotaEsperada();
        dto.incompleto = a.esIncompleto();
        dto.fecha = a.getFecha();
        dto.fechaRegistro = a.getFechaRegistro();
        dto.observacion = a.getObservacion();
        return dto;
    }

    public Long getId() { return id; }
    public Long getPrestamoId() { return prestamoId; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public Long getCobradorId() { return cobradorId; }
    public String getCobradorNombre() { return cobradorNombre; }
    public Double getMonto() { return monto; }
    public Double getCuotaEsperada() { return cuotaEsperada; }
    public boolean isIncompleto() { return incompleto; }
    public LocalDate getFecha() { return fecha; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public String getObservacion() { return observacion; }

    public Integer getCuotasCubiertas() { return cuotasCubiertas; }
    public void setCuotasCubiertas(Integer cuotasCubiertas) { this.cuotasCubiertas = cuotasCubiertas; }

    public Double getSaldoPrestamo() { return saldoPrestamo; }
    public void setSaldoPrestamo(Double saldoPrestamo) { this.saldoPrestamo = saldoPrestamo; }
}
