package com.example.cobro.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Balance de caja de un cobrador para un día (abierta = vista previa, cerrada = cuadre registrado). */
public record CuadreCajaDTO(
        Long id,
        Long cobradorId,
        String cobradorNombre,
        String zona,
        LocalDate fecha,
        String estado,
        double baseInicial,
        double totalCobrado,
        int numeroAbonos,
        double totalPrestado,
        int numeroPrestamos,
        double gastos,
        double efectivoEsperado,
        Double efectivoEntregado,
        Double diferencia,
        String resultado,
        String observacion,
        LocalDateTime fechaCierre,
        List<AbonoResponseDTO> abonos,
        List<Desembolso> desembolsos
) {
    public static final String ABIERTA = "ABIERTA";
    public static final String CERRADA = "CERRADA";

    public record Desembolso(Long prestamoId, String clienteNombre, double monto) {}

    /** CUADRADA, SOBRANTE o FALTANTE según la diferencia; null si la caja sigue abierta. */
    public static String resultadoDe(Double diferencia) {
        if (diferencia == null) return null;
        if (Math.abs(diferencia) < 0.01) return "CUADRADA";
        return diferencia > 0 ? "SOBRANTE" : "FALTANTE";
    }
}
