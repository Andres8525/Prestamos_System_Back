package com.example.cobro.tiemporeal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reenvía los eventos de dominio a los clientes WebSocket solo cuando la transacción se confirma:
 * nunca se anuncia un abono que terminó en rollback. Sin transacción activa se envía de inmediato.
 */
@Component
public class OyenteEventosNegocio {

    private final DifusorEventos difusor;

    public OyenteEventosNegocio(DifusorEventos difusor) {
        this.difusor = difusor;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alConfirmar(EventoNegocio evento) {
        difusor.difundir(evento.tipo(), evento.mensaje(), evento.datos());
    }
}
