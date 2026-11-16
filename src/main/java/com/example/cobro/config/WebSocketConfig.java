package com.example.cobro.config;

import com.example.cobro.tiemporeal.AutenticacionHandshake;
import com.example.cobro.tiemporeal.ManejadorEventosWebSocket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

/** Registra el canal de eventos en tiempo real {@code /ws/eventos} con los mismos orígenes que CORS. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ManejadorEventosWebSocket manejador;
    private final AutenticacionHandshake autenticacion;
    private final String[] origenes;

    public WebSocketConfig(ManejadorEventosWebSocket manejador, AutenticacionHandshake autenticacion,
                           @Value("${allowed.origins}") String origenes) {
        this.manejador = manejador;
        this.autenticacion = autenticacion;
        this.origenes = Arrays.stream(origenes.split(",")).map(String::trim).toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
        registro.addHandler(manejador, "/ws/eventos")
                .addInterceptors(autenticacion)
                .setAllowedOriginPatterns(origenes);
    }
}
