package com.example.cobro.grpc;

import com.example.cobro.security.TokenService;
import io.grpc.*;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Exige el mismo token de sesión que la API REST, enviado en el metadato {@code authorization: Bearer ...}.
 * Los servicios de health y reflection quedan libres para herramientas como grpcurl y balanceadores.
 * El id del usuario autenticado queda disponible en {@link #USUARIO_ID} durante la llamada.
 */
@Component
public class AutenticacionGrpcInterceptor implements ServerInterceptor {

    public static final Context.Key<Long> USUARIO_ID = Context.key("usuarioId");
    static final Metadata.Key<String> AUTORIZACION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final Set<String> SERVICIOS_PUBLICOS = Set.of(
            "grpc.health.v1.Health", "grpc.reflection.v1.ServerReflection", "grpc.reflection.v1alpha.ServerReflection");

    private final TokenService tokenService;

    public AutenticacionGrpcInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> llamada, Metadata cabeceras,
                                                       ServerCallHandler<Q, R> siguiente) {
        if (SERVICIOS_PUBLICOS.contains(llamada.getMethodDescriptor().getServiceName())) {
            return siguiente.startCall(llamada, cabeceras);
        }
        String valor = cabeceras.get(AUTORIZACION);
        Optional<TokenService.Sesion> sesion = valor != null && valor.startsWith("Bearer ")
                ? tokenService.validar(valor.substring(7).trim())
                : Optional.empty();
        if (sesion.isEmpty()) {
            llamada.close(Status.UNAUTHENTICATED.withDescription("Debe iniciar sesión para continuar"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        Context contexto = Context.current().withValue(USUARIO_ID, sesion.get().usuarioId());
        return Contexts.interceptCall(contexto, llamada, cabeceras, siguiente);
    }
}
