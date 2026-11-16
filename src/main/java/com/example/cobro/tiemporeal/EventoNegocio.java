package com.example.cobro.tiemporeal;

import java.util.Map;

/**
 * Evento de dominio publicado por los servicios con {@code ApplicationEventPublisher}.
 * Los servicios no conocen el WebSocket: solo anuncian qué pasó (bajo acoplamiento).
 */
public record EventoNegocio(TipoEvento tipo, String mensaje, Map<String, Object> datos) {

    public EventoNegocio {
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }
}
