package com.example.cobro.grpc;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registra cada llamada gRPC: método, código de estado y duración (incluye la duración completa de los streams).
 */
@Component
public class RegistroLlamadasInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RegistroLlamadasInterceptor.class);

    private final MetricasGrpc metricas;

    public RegistroLlamadasInterceptor(MetricasGrpc metricas) {
        this.metricas = metricas;
    }

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> llamada, Metadata cabeceras,
                                                       ServerCallHandler<Q, R> siguiente) {
        long inicio = System.nanoTime();
        String metodo = llamada.getMethodDescriptor().getFullMethodName();
        ServerCall<Q, R> medida = new ForwardingServerCall.SimpleForwardingServerCall<>(llamada) {
            @Override
            public void close(Status estado, Metadata finales) {
                long ms = (System.nanoTime() - inicio) / 1_000_000;
                metricas.registrar(metodo, estado.isOk(), ms, estado.getCode().name());
                log.debug("gRPC {} -> {} en {} ms", metodo, estado.getCode(), ms);
                super.close(estado, finales);
            }
        };
        return siguiente.startCall(medida, cabeceras);
    }
}
