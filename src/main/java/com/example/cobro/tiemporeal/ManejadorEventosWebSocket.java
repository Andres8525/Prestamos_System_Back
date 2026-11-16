package com.example.cobro.tiemporeal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Canal {@code /ws/eventos}. Al conectar envía la bienvenida y los eventos perdidos desde {@code desde};
 * acepta del cliente las acciones {@code ping}, {@code suscribir} y {@code desuscribir} (con {@code tipos}).
 */
@Component
public class ManejadorEventosWebSocket extends TextWebSocketHandler {

    private static final int LIMITE_ENVIO_MS = 10_000;
    private static final int LIMITE_BUFFER_BYTES = 512 * 1024;
    private static final int EVENTOS_INICIALES = 10;
    private static final int LIMITE_MENSAJE_ENTRANTE = 16 * 1024;

    private final RegistroConexiones conexiones;
    private final DifusorEventos difusor;
    private final HistorialEventos historial;
    private final ObjectMapper json;

    public ManejadorEventosWebSocket(RegistroConexiones conexiones, DifusorEventos difusor,
                                     HistorialEventos historial, ObjectMapper json) {
        this.conexiones = conexiones;
        this.difusor = difusor;
        this.historial = historial;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) {
        sesion.setTextMessageSizeLimit(LIMITE_MENSAJE_ENTRANTE);
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(sesion, LIMITE_ENVIO_MS, LIMITE_BUFFER_BYTES);
        Map<String, Object> atributos = sesion.getAttributes();
        ConexionCliente conexion = new ConexionCliente(segura, (Long) atributos.get(AutenticacionHandshake.USUARIO_ID),
                (String) atributos.get(AutenticacionHandshake.EMAIL));
        conexiones.agregar(conexion);

        difusor.enviarA(conexion, difusor.crear(TipoEvento.BIENVENIDA, "Conectado a CrediRuta en tiempo real",
                Map.of("ultimoId", difusor.getUltimoId(), "conexiones", conexiones.cantidad())));
        long desde = (Long) atributos.getOrDefault(AutenticacionHandshake.DESDE, 0L);
        List<EventoTiempoReal> pendientes = desde > 0 ? historial.desde(desde) : historial.recientes(EVENTOS_INICIALES);
        pendientes.forEach(e -> difusor.enviarA(conexion, e));
        anunciarConexiones();
    }

    @Override
    protected void handleTextMessage(WebSocketSession sesion, TextMessage mensaje) throws Exception {
        ConexionCliente conexion = conexiones.obtener(sesion.getId()).orElse(null);
        if (conexion == null) return;
        JsonNode nodo = json.readTree(mensaje.getPayload());
        switch (nodo.path("accion").asText()) {
            case "ping" -> difusor.enviarA(conexion, difusor.crear(TipoEvento.LATIDO, "pong", Map.of()));
            case "suscribir" -> conexion.suscribir(tipos(nodo));
            case "desuscribir" -> conexion.desuscribir(tipos(nodo));
            default -> {
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, CloseStatus estado) {
        conexiones.quitar(sesion.getId());
        anunciarConexiones();
    }

    @Override
    public void handleTransportError(WebSocketSession sesion, Throwable error) {
        conexiones.quitar(sesion.getId());
    }

    private void anunciarConexiones() {
        difusor.difundir(TipoEvento.CONEXIONES, "Usuarios conectados",
                Map.of("conexiones", conexiones.cantidad(), "usuarios", conexiones.usuariosDistintos()));
    }

    private static List<TipoEvento> tipos(JsonNode nodo) {
        List<TipoEvento> tipos = new ArrayList<>();
        for (JsonNode t : nodo.path("tipos")) {
            try {
                tipos.add(TipoEvento.valueOf(t.asText()));
            } catch (IllegalArgumentException desconocido) {
                // Se ignoran los tipos que el servidor no conoce.
            }
        }
        return tipos;
    }
}
