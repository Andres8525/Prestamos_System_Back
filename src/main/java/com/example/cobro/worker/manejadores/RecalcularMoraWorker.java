package com.example.cobro.worker.manejadores;

import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Prestamo;
import com.example.cobro.repository.PrestamoRepository;
import com.example.cobro.service.PrestamoService;
import com.example.cobro.worker.ManejadorTarea;
import com.example.cobro.worker.TipoTarea;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/** Recalcula el estado de mora de cada préstamo que no está cancelado. */
@Component
public class RecalcularMoraWorker implements ManejadorTarea {

    private final PrestamoRepository prestamoRepository;
    private final PrestamoService prestamoService;

    public RecalcularMoraWorker(PrestamoRepository prestamoRepository, PrestamoService prestamoService) {
        this.prestamoRepository = prestamoRepository;
        this.prestamoService = prestamoService;
    }

    @Override
    public TipoTarea tipo() {
        return TipoTarea.RECALCULAR_MORA;
    }

    @Override
    public Resultado ejecutar() {
        LocalDate hoy = LocalDate.now();
        int revisados = 0, cambios = 0;
        Map<EstadoPago, Integer> porEstado = new EnumMap<>(EstadoPago.class);
        for (Prestamo p : prestamoRepository.findAll()) {
            if (p.getEstado() == EstadoPago.PAGADO) continue;
            EstadoPago antes = p.getEstado();
            EstadoPago despues = prestamoService.recalcularEstado(p, hoy).getEstado();
            revisados++;
            if (antes != despues) cambios++;
            porEstado.merge(despues, 1, Integer::sum);
        }
        return Resultado.de(revisados + " préstamos revisados, " + cambios + " cambiaron de estado " + porEstado);
    }
}
