package com.example.cobro.tiemporeal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Crea los eventos con su secuencia y los envía a todas las conexiones suscritas. */
@Component
public class DifusorEventos {

    private static final Logger log = LoggerFactory.getLogger(DifusorEventos.class);

    private final RegistroConexiones conexiones;
    private final HistorialEventos historial;
    private final ObjectMapper json;
    private final AtomicLong secuencia = new AtomicLong();
    private final AtomicLong enviados = new AtomicLong();

    public DifusorEventos(RegistroConexiones conexiones, HistorialEventos historial, ObjectMapper json) {
        this.conexiones = conexiones;
        this.historial = historial;
        this.json = json;
    }

    public EventoTiempoReal crear(TipoEvento tipo, String mensaje, Map<String, Object> datos) {
        return new EventoTiempoReal(secuencia.incrementAndGet(), tipo, mensaje, datos, Instant.now());
    }

    public EventoTiempoReal difundir(TipoEvento tipo, String mensaje, Map<String, Object> datos) {
        EventoTiempoReal evento = crear(tipo, mensaje, datos);
        if (!tipo.esSistema()) historial.agregar(evento);
        TextMessage texto = serializar(evento);
        for (ConexionCliente c : conexiones.todas()) {
            if (c.suscrita(tipo)) enviar(c, texto);
        }
        return evento;
    }

    public void enviarA(ConexionCliente conexion, EventoTiempoReal evento) {
        enviar(conexion, serializar(evento));
    }

    private void enviar(ConexionCliente c, TextMessage texto) {
        try {
            if (c.sesion().isOpen()) {
                c.sesion().sendMessage(texto);
                enviados.incrementAndGet();
            }
        } catch (Exception e) {
            log.debug("Se descarta la conexión {}: {}", c.sesion().getId(), e.getMessage());
            conexiones.quitar(c.sesion().getId());
        }
    }

    private TextMessage serializar(EventoTiempoReal evento) {
        try {
            return new TextMessage(json.writeValueAsString(evento));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el evento " + evento.tipo(), e);
        }
    }

    public long getEnviados() { return enviados.get(); }
    public long getUltimoId() { return secuencia.get(); }
}
