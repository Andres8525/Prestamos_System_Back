package com.example.cobro.groq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de consultas al asistente por usuario (ventana deslizante de 1 minuto) para no agotar
 * la cuota de Groq. Solo cuenta las consultas que realmente se envían a Groq.
 */
@Component
public class LimiteSolicitudesIa {

    private static final long VENTANA_MS = 60_000;

    private final int maximoPorMinuto;
    private final Map<Long, Deque<Long>> historial = new ConcurrentHashMap<>();

    public LimiteSolicitudesIa(@Value("${app.groq.limite-por-minuto:10}") int maximoPorMinuto) {
        this.maximoPorMinuto = Math.max(1, maximoPorMinuto);
    }

    /** @return true si el usuario puede hacer otra consulta ahora (y la registra). */
    public boolean permitir(Long usuarioId) {
        long ahora = System.currentTimeMillis();
        Deque<Long> marcas = historial.computeIfAbsent(usuarioId == null ? -1L : usuarioId, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahora - marcas.peekFirst() > VENTANA_MS) marcas.pollFirst();
            if (marcas.size() >= maximoPorMinuto) return false;
            marcas.addLast(ahora);
            return true;
        }
    }
}
