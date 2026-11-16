package com.example.cobro.tiemporeal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Búfer circular con los últimos eventos de negocio (estructura de tamaño fijo: al llenarse descarta el más
 * antiguo en O(1)). Permite que un cliente que se conecta o reconecta reciba lo que se perdió.
 */
@Component
public class HistorialEventos {

    private final int capacidad;
    private final Deque<EventoTiempoReal> eventos;

    public HistorialEventos(@Value("${app.websocket.historial:50}") int capacidad) {
        this.capacidad = Math.max(1, capacidad);
        this.eventos = new ArrayDeque<>(this.capacidad);
    }

    public synchronized void agregar(EventoTiempoReal evento) {
        if (eventos.size() == capacidad) eventos.pollFirst();
        eventos.addLast(evento);
    }

    /** Eventos con id mayor que {@code ultimoId}, del más antiguo al más reciente. */
    public synchronized List<EventoTiempoReal> desde(long ultimoId) {
        List<EventoTiempoReal> resultado = new ArrayList<>();
        for (EventoTiempoReal e : eventos) {
            if (e.id() > ultimoId) resultado.add(e);
        }
        return resultado;
    }

    public synchronized List<EventoTiempoReal> recientes(int cantidad) {
        List<EventoTiempoReal> todos = new ArrayList<>(eventos);
        return todos.subList(Math.max(0, todos.size() - cantidad), todos.size());
    }

    public synchronized int tamano() {
        return eventos.size();
    }
}
