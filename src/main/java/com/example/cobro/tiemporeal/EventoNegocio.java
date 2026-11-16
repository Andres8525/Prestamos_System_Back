package com.example.cobro.tiemporeal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Evento de dominio publicado por los servicios con {@code ApplicationEventPublisher}.
 * Los servicios no conocen el WebSocket: solo anuncian qué pasó (bajo acoplamiento).
 */
public record EventoNegocio(TipoEvento tipo, String mensaje, Map<String, Object> datos) {

    public EventoNegocio {
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }

    /** Crea el evento a partir de pares clave, valor; los valores nulos se omiten. */
    public static EventoNegocio de(TipoEvento tipo, String mensaje, Object... claveValor) {
        Map<String, Object> datos = new LinkedHashMap<>();
        for (int i = 0; i + 1 < claveValor.length; i += 2) {
            if (claveValor[i + 1] != null) datos.put(String.valueOf(claveValor[i]), claveValor[i + 1]);
        }
        return new EventoNegocio(tipo, mensaje, datos);
    }
}
