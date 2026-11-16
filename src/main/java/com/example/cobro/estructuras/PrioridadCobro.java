package com.example.cobro.estructuras;

import com.example.cobro.dto.RutaCobroDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Ordena las paradas de una ruta por urgencia con una cola de prioridad (montículo binario, O(n log n)):
 * <ol>
 *   <li>primero las que tienen cobro exigible hoy;</li>
 *   <li>luego más días de atraso;</li>
 *   <li>luego mayor monto exigible.</li>
 * </ol>
 * Las paradas sin nada que cobrar quedan al final, en su orden de visita original.
 */
public final class PrioridadCobro {

    public static final Comparator<RutaCobroDTO.Parada> URGENCIA = Comparator
            .comparing(RutaCobroDTO.Parada::cobraHoy).reversed()
            .thenComparing(Comparator.comparingLong(RutaCobroDTO.Parada::diasAtraso).reversed())
            .thenComparing(Comparator.comparingDouble(RutaCobroDTO.Parada::montoExigible).reversed())
            .thenComparingInt(RutaCobroDTO.Parada::orden);

    private PrioridadCobro() {}

    /** Devuelve las paradas en orden de urgencia, renumeradas desde 1. */
    public static List<RutaCobroDTO.Parada> ordenar(List<RutaCobroDTO.Parada> paradas) {
        PriorityQueue<RutaCobroDTO.Parada> cola = new PriorityQueue<>(Math.max(1, paradas.size()), URGENCIA);
        cola.addAll(paradas);
        List<RutaCobroDTO.Parada> ordenadas = new ArrayList<>(paradas.size());
        int orden = 1;
        while (!cola.isEmpty()) {
            RutaCobroDTO.Parada p = cola.poll();
            ordenadas.add(new RutaCobroDTO.Parada(orden++, p.clienteId(), p.clienteNombre(), p.direccion(),
                    p.telefono(), p.prestamoId(), p.tipoLiquidacion(), p.estado(), p.valorCuota(), p.montoExigible(),
                    p.cuotasVencidas(), p.diasAtraso(), p.saldoPendiente(), p.abonadoHoy(), p.cobraHoy()));
        }
        return ordenadas;
    }
}
