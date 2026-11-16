package com.example.cobro.dto;

import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.TipoLiquidacion;
import java.time.LocalDate;
import java.util.List;

/** Hoja de ruta diaria de un cobrador: clientes a visitar y montos a recaudar. */
public record RutaCobroDTO(
        Long cobradorId,
        String cobradorNombre,
        String zona,
        LocalDate fecha,
        int totalVisitas,
        int visitasConAbono,
        double totalExigible,
        double totalRecaudado,
        List<Parada> paradas
) {
    /** Una parada de la ruta: un préstamo activo de un cliente. */
    public record Parada(
            int orden,
            Long clienteId,
            String clienteNombre,
            String direccion,
            String telefono,
            Long prestamoId,
            TipoLiquidacion tipoLiquidacion,
            EstadoPago estado,
            double valorCuota,
            double montoExigible,
            int cuotasVencidas,
            long diasAtraso,
            double saldoPendiente,
            double abonadoHoy,
            boolean cobraHoy
    ) {}
}
