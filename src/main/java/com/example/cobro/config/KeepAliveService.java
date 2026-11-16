package com.example.cobro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mantiene despierto el servicio en el plan gratuito de Render.
 *
 * <p>Render suspende un servicio gratuito tras 15 minutos sin tráfico entrante y el siguiente
 * login tiene que esperar a que vuelva a arrancar (≈ 1 minuto). Este componente se llama a sí mismo
 * a través de su URL pública ({@code RENDER_EXTERNAL_URL}, que Render define automáticamente) cada
 * pocos minutos: la petición entra por el balanceador de Render y cuenta como tráfico.</p>
 *
 * <p>Si no hay URL pública (desarrollo local) no hace nada.</p>
 */
@Component
@ConditionalOnProperty(name = "app.keepalive.habilitado", havingValue = "true", matchIfMissing = true)
public class KeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveService.class);

    private final String urlPublica;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AtomicLong exitos = new AtomicLong();
    private final AtomicLong fallos = new AtomicLong();
    private final AtomicReference<String> ultimoResultado = new AtomicReference<>("sin ejecutar");

    public KeepAliveService(@Value("${app.keepalive.url:}") String urlPublica) {
        this.urlPublica = urlPublica == null ? "" : urlPublica.trim().replaceAll("/+$", "");
    }

    public boolean activo() {
        return !urlPublica.isEmpty();
    }

    /** Se ejecuta cada {@code app.keepalive.intervalo-ms} (10 minutos por defecto, menos que los 15 de Render). */
    @Scheduled(initialDelayString = "${app.keepalive.retardo-inicial-ms:60000}",
               fixedDelayString = "${app.keepalive.intervalo-ms:600000}")
    public void mantenerDespierto() {
        if (!activo()) return;
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlPublica + "/api/ping"))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "crediruta-keepalive")
                    .GET().build();
            int codigo = http.send(peticion, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (codigo < 400) exitos.incrementAndGet(); else fallos.incrementAndGet();
            ultimoResultado.set("HTTP " + codigo);
        } catch (Exception e) {
            fallos.incrementAndGet();
            ultimoResultado.set("error: " + e.getClass().getSimpleName());
            log.debug("Keep-alive sin respuesta: {}", e.getMessage());
        }
    }

    public long getExitos() { return exitos.get(); }
    public long getFallos() { return fallos.get(); }
    public String getUltimoResultado() { return ultimoResultado.get(); }
    public String getUrlPublica() { return urlPublica; }
}
