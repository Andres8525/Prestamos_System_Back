package com.example.cobro.tiemporeal;

import java.time.Instant;
import java.util.Map;

/**
 * Mensaje inmutable enviado a los clientes WebSocket.
 *
 * @param id     secuencia creciente: el cliente detecta eventos perdidos al reconectar
 * @param datos  carga útil (ids, montos, estados); nunca datos sensibles del cliente
 */
public record EventoTiempoReal(long id, TipoEvento tipo, String mensaje, Map<String, Object> datos, Instant fecha) {

    public EventoTiempoReal {
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }
}
