package com.example.cobro.controller;

import com.example.cobro.config.KeepAliveService;
import com.example.cobro.tiemporeal.RegistroConexiones;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verificación de disponibilidad (pública). La usan el health check de Render, el keep-alive
 * del propio servidor, el workflow de GitHub Actions y el frontend para despertar el servicio.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;
    private final ObjectProvider<KeepAliveService> keepAlive;
    private final RegistroConexiones conexiones;

    public HealthController(DataSource dataSource, ObjectProvider<KeepAliveService> keepAlive,
                            RegistroConexiones conexiones) {
        this.dataSource = dataSource;
        this.keepAlive = keepAlive;
        this.conexiones = conexiones;
    }

    /**
     * Estado del servicio y de la base de datos. Responde siempre 200 mientras el proceso esté vivo:
     * una caída momentánea de la base no debe hacer que Render reinicie la aplicación.
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("status", "UP");
        estado.put("baseDatos", baseDisponible() ? "UP" : "DOWN");
        estado.put("hora", Instant.now().toString());
        estado.put("conexionesTiempoReal", conexiones.cantidad());
        estado.put("activoDesde", Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).toSeconds());
        KeepAliveService ka = keepAlive.getIfAvailable();
        if (ka != null && ka.activo()) {
            estado.put("keepAlive", Map.of("exitos", ka.getExitos(), "fallos", ka.getFallos(),
                    "ultimo", ka.getUltimoResultado()));
        }
        return estado;
    }

    /** Respuesta mínima para monitores externos que solo necesitan saber si el proceso responde. */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("pong", Instant.now().toString());
    }

    private boolean baseDisponible() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
