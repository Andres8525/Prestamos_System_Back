package com.example.cobro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
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

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("status", "UP");
        estado.put("hora", Instant.now().toString());
        estado.put("activoDesde", Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).toSeconds());
        return estado;
    }
}
