package com.example.cobro.ia;

import com.example.cobro.tiemporeal.EventoNegocio;
import com.example.cobro.tiemporeal.TipoEvento;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

/** Invalida el scoring en caché de un cliente cuando se confirma un abono o un préstamo suyo. */
@Component
public class InvalidadorCacheScoring {

    private static final Set<TipoEvento> AFECTAN_RIESGO = Set.of(TipoEvento.ABONO_REGISTRADO, TipoEvento.PRESTAMO_CREADO);

    private final CreditScoringService scoringService;

    public InvalidadorCacheScoring(CreditScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alConfirmar(EventoNegocio evento) {
        if (AFECTAN_RIESGO.contains(evento.tipo()) && evento.datos().get("clienteId") instanceof Number id) {
            scoringService.invalidarCliente(id.longValue());
        }
    }
}
