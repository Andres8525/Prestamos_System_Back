package com.example.cobro.tiemporeal;

import com.example.cobro.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * Valida el token de sesión antes de abrir el WebSocket. Los navegadores no permiten cabeceras propias en el
 * handshake, por eso el token llega en el parámetro {@code token} (solo por HTTPS en producción).
 */
@Component
public class AutenticacionHandshake implements HandshakeInterceptor {

    static final String USUARIO_ID = "usuarioId";
    static final String EMAIL = "email";
    static final String DESDE = "desde";

    private final TokenService tokenService;

    public AutenticacionHandshake(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> atributos) {
        var parametros = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        Optional<TokenService.Sesion> sesion = Optional.ofNullable(parametros.getFirst("token"))
                .flatMap(tokenService::validar);
        if (sesion.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        atributos.put(USUARIO_ID, sesion.get().usuarioId());
        atributos.put(EMAIL, sesion.get().email());
        atributos.put(DESDE, parsearDesde(parametros.getFirst("desde")));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception error) {
    }

    private static long parsearDesde(String valor) {
        try {
            return valor == null ? 0 : Math.max(0, Long.parseLong(valor));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
