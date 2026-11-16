package com.example.cobro.tiemporeal;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Envía un latido a todas las conexiones cada {@code app.websocket.latido-ms}: los proxies (como el de Render)
 * cierran los WebSocket inactivos, y el cliente usa el latido para detectar una conexión caída.
 */
@Component
public class LatidoWebSocket {

    private final RegistroConexiones conexiones;
    private final DifusorEventos difusor;

    public LatidoWebSocket(RegistroConexiones conexiones, DifusorEventos difusor) {
        this.conexiones = conexiones;
        this.difusor = difusor;
    }

    @Scheduled(fixedDelayString = "${app.websocket.latido-ms:25000}", initialDelayString = "${app.websocket.latido-ms:25000}")
    public void latir() {
        if (conexiones.cantidad() == 0) return;
        difusor.difundir(TipoEvento.LATIDO, "latido", Map.of("conexiones", conexiones.cantidad()));
    }
}
