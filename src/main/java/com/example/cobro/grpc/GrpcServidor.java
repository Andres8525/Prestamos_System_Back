package com.example.cobro.grpc;

import io.grpc.*;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor gRPC embebido. Arranca junto con Spring en su propio puerto ({@code app.grpc.puerto}, 9090 por defecto)
 * y publica:
 * <ul>
 *   <li>los servicios de negocio ({@link ScoringGrpcService}, {@link CobranzaGrpcService}) con los
 *       interceptores de registro y autenticación;</li>
 *   <li>el servicio estándar de health ({@code grpc.health.v1.Health});</li>
 *   <li>reflection, para explorar los servicios con herramientas como grpcurl o Postman.</li>
 * </ul>
 */
@Component
public class GrpcServidor implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcServidor.class);

    private final List<BindableService> servicios;
    private final AutenticacionGrpcInterceptor autenticacion;
    private final RegistroLlamadasInterceptor registro;
    private final boolean habilitado;
    private final int puertoConfigurado;
    private final int hilos;

    private final HealthStatusManager health = new HealthStatusManager();
    private Server servidor;
    private ExecutorService ejecutor;

    public GrpcServidor(List<BindableService> servicios, AutenticacionGrpcInterceptor autenticacion,
                        RegistroLlamadasInterceptor registro,
                        @Value("${app.grpc.habilitado:true}") boolean habilitado,
                        @Value("${app.grpc.puerto:9090}") int puerto,
                        @Value("${app.grpc.hilos:4}") int hilos) {
        this.servicios = servicios;
        this.autenticacion = autenticacion;
        this.registro = registro;
        this.habilitado = habilitado;
        this.puertoConfigurado = puerto;
        this.hilos = Math.max(1, hilos);
    }

    @Override
    public synchronized void start() {
        if (!habilitado || servidor != null) return;
        AtomicInteger contador = new AtomicInteger();
        ejecutor = Executors.newFixedThreadPool(hilos, r -> {
            Thread t = new Thread(r, "grpc-" + contador.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        ServerBuilder<?> builder = Grpc.newServerBuilderForPort(puertoConfigurado, InsecureServerCredentials.create())
                .executor(ejecutor)
                .maxInboundMessageSize(4 * 1024 * 1024)
                .addService(ServerInterceptors.intercept(health.getHealthService(), registro))
                .addService(ProtoReflectionServiceV1.newInstance());
        // El último interceptor se ejecuta primero: registro envuelve a autenticación (se miden también los rechazos).
        servicios.forEach(s -> builder.addService(ServerInterceptors.intercept(s, autenticacion, registro)));
        try {
            servidor = builder.build().start();
        } catch (IOException e) {
            log.error("No se pudo iniciar el servidor gRPC en el puerto {}: {}", puertoConfigurado, e.getMessage());
            servidor = null;
            ejecutor.shutdownNow();
            return;
        }
        health.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
        servicios.forEach(s -> health.setStatus(s.bindService().getServiceDescriptor().getName(),
                HealthCheckResponse.ServingStatus.SERVING));
        log.info("Servidor gRPC escuchando en el puerto {} con {} servicios", servidor.getPort(), servicios.size());
    }

    @Override
    public synchronized void stop() {
        if (servidor == null) return;
        health.enterTerminalState();
        servidor.shutdown();
        try {
            if (!servidor.awaitTermination(5, TimeUnit.SECONDS)) servidor.shutdownNow();
        } catch (InterruptedException e) {
            servidor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        ejecutor.shutdownNow();
        servidor = null;
    }

    @Override
    public boolean isRunning() {
        return servidor != null && !servidor.isShutdown();
    }

    /** Puerto real (útil con puerto 0 en pruebas) o -1 si el servidor no está activo. */
    public int getPuerto() {
        return isRunning() ? servidor.getPort() : -1;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    /** Definiciones de los servicios publicados (incluye health y reflection). */
    public List<ServerServiceDefinition> getServicios() {
        return isRunning() ? servidor.getServices() : List.of();
    }
}
