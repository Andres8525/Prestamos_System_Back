package com.example.cobro.dto;

import java.time.LocalDate;

/**
 * Resumen de las cuotas pendientes de un cliente (todas sus deudas vigentes juntas).
 *
 * @param cuotasAtrasadas cuotas cuya fecha programada ya pasó y siguen sin pagarse
 * @param proximaFecha    fecha programada de la cuota pendiente más antigua
 */
public record CuotasClienteDTO(Long clienteId, String clienteNombre, String cedula, int prestamos,
                               int cuotasPendientes, int cuotasAtrasadas, double saldoPendiente,
                               LocalDate proximaFecha) {
}
