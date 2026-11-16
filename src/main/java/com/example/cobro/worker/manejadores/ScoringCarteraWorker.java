package com.example.cobro.worker.manejadores;

import com.example.cobro.ia.CreditScoringService;
import com.example.cobro.ia.HistorialMlService;
import com.example.cobro.ia.NivelRiesgo;
import com.example.cobro.ia.ScoringDTO;
import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.PrediccionRiesgo;
import com.example.cobro.model.Prestamo;
import com.example.cobro.repository.PrestamoRepository;
import com.example.cobro.worker.ManejadorTarea;
import com.example.cobro.worker.TipoTarea;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Evalúa con el modelo de ML a todos los clientes con crédito activo y guarda la predicción de seguimiento. */
@Component
public class ScoringCarteraWorker implements ManejadorTarea {

    private final PrestamoRepository prestamoRepository;
    private final CreditScoringService scoringService;
    private final HistorialMlService historialMl;

    public ScoringCarteraWorker(PrestamoRepository prestamoRepository, CreditScoringService scoringService,
                                HistorialMlService historialMl) {
        this.prestamoRepository = prestamoRepository;
        this.scoringService = scoringService;
        this.historialMl = historialMl;
    }

    @Override
    public TipoTarea tipo() {
        return TipoTarea.SCORING_CARTERA;
    }

    @Override
    public Resultado ejecutar() {
        Set<Long> clientes = prestamoRepository.findAll().stream()
                .filter(p -> p.getEstado() != EstadoPago.PAGADO)
                .map(Prestamo::getClienteId)
                .collect(Collectors.toSet());
        Map<NivelRiesgo, Integer> porNivel = new EnumMap<>(NivelRiesgo.class);
        for (Long id : clientes) {
            ScoringDTO s = scoringService.evaluarCliente(id);
            historialMl.registrarPrediccion(s, PrediccionRiesgo.Origen.SEGUIMIENTO);
            porNivel.merge(s.nivelRiesgo(), 1, Integer::sum);
        }
        return Resultado.de(clientes.size() + " clientes evaluados " + porNivel);
    }
}
