package com.example.cobro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Exige un token válido (Authorization: Bearer ...) en toda la API,
 * excepto en el registro, el inicio de sesión (y su paso de verificación del email)
 * y la verificación de disponibilidad.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_USUARIO_ID = "usuarioId";
    private static final Set<String> RUTAS_PUBLICAS =
            Set.of("/api/auth/login", "/api/auth/register", "/api/auth/verificar-email",
                    "/api/health", "/api/ping");

    private final TokenService tokenService;

    public AuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !ruta.startsWith("/api/")
                || RUTAS_PUBLICAS.contains(ruta);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        Optional<TokenService.Sesion> sesion = cabecera != null && cabecera.startsWith("Bearer ")
                ? tokenService.validar(cabecera.substring(7).trim())
                : Optional.empty();

        if (sesion.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Debe iniciar sesión para continuar\"}");
            return;
        }
        request.setAttribute(ATRIBUTO_USUARIO_ID, sesion.get().usuarioId());
        chain.doFilter(request, response);
    }
}
