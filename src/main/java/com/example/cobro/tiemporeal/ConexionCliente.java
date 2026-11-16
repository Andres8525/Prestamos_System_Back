package com.example.cobro.tiemporeal;

import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Conexión WebSocket autenticada y los tipos de evento a los que está suscrita. */
public final class ConexionCliente {

    private final WebSocketSession sesion;
    private final Long usuarioId;
    private final String email;
    private final Instant conectadoEn = Instant.now();
    private final Set<TipoEvento> suscripciones = EnumSet.allOf(TipoEvento.class);

    public ConexionCliente(WebSocketSession sesion, Long usuarioId, String email) {
        this.sesion = sesion;
        this.usuarioId = usuarioId;
        this.email = email;
    }

    public synchronized boolean suscrita(TipoEvento tipo) {
        return tipo.esSistema() || suscripciones.contains(tipo);
    }

    public synchronized void suscribir(Collection<TipoEvento> tipos) {
        suscripciones.addAll(tipos);
    }

    public synchronized void desuscribir(Collection<TipoEvento> tipos) {
        suscripciones.removeAll(tipos);
    }

    public WebSocketSession sesion() { return sesion; }
    public Long usuarioId() { return usuarioId; }
    public String email() { return email; }
    public Instant conectadoEn() { return conectadoEn; }
}
