package com.example.cobro.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Cliente HTTP mínimo de la API de chat de Groq (sin SDK: java.net.http + Jackson).
 * Si Groq retira el modelo configurado, prueba los de respaldo y se queda con el primero que responda.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    /** Error al llamar a Groq (clave inválida, límite de uso, caída del servicio, tiempo agotado…). */
    public static class GroqException extends RuntimeException {
        private final int codigoHttp;

        public GroqException(String mensaje, int codigoHttp, Throwable causa) {
            super(mensaje, causa);
            this.codigoHttp = codigoHttp;
        }

        public int getCodigoHttp() { return codigoHttp; }
    }

    /** Groq respondió que el modelo no existe o fue retirado: se puede probar con otro. */
    private static final class ModeloNoDisponible extends Exception {
        private final GroqException error;

        ModeloNoDisponible(GroqException error) {
            super(error.getMessage(), null, false, false);
            this.error = error;
        }
    }

    private final GroqProperties config;
    private final ObjectMapper json;
    private final HttpClient http;
    private volatile String modeloActivo;
    private volatile boolean modeloVerificado;

    public GroqClient(GroqProperties config, ObjectMapper json) {
        this.config = config;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(config.timeout()).build();
        this.modeloActivo = config.modelo();
    }

    public boolean configurado() {
        return config.configurado();
    }

    /** Modelo en uso: el configurado o el de respaldo que respondió por última vez. */
    public String modelo() {
        return modeloActivo;
    }

    /**
     * Consulta una vez el catálogo de Groq (/models) y fija como activo el primer candidato disponible,
     * para que el estado muestre el modelo real antes de la primera pregunta. Si falla, no cambia nada.
     */
    public void verificarModelo() {
        if (modeloVerificado || !configurado()) return;
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(config.urlBase() + "/models"))
                    .timeout(config.timeout()).header("Authorization", "Bearer " + config.apiKey()).GET().build();
            HttpResponse<String> r = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() != 200) return;
            Set<String> disponibles = new java.util.HashSet<>();
            json.readTree(r.body()).path("data").forEach(m -> disponibles.add(m.path("id").asText()));
            candidatos().stream().filter(disponibles::contains).findFirst().ifPresent(m -> {
                if (!m.equals(modeloActivo)) log.warn("Groq: el modelo {} no está disponible; se usa {}", modeloActivo, m);
                modeloActivo = m;
            });
            modeloVerificado = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception sinCatalogo) {
            log.debug("Groq: no se pudo consultar el catálogo de modelos", sinCatalogo);
        }
    }

    private Set<String> candidatos() {
        Set<String> candidatos = new LinkedHashSet<>();
        candidatos.add(modeloActivo);
        candidatos.add(config.modelo());
        candidatos.add(GroqProperties.MODELO_POR_DEFECTO);
        candidatos.addAll(config.modelosRespaldo());
        return candidatos;
    }

    public GroqDTO.Respuesta chat(List<GroqDTO.Mensaje> mensajes) {
        if (!configurado()) {
            throw new GroqException("La API de Groq no está configurada (defina GROQ_API_KEY en el backend)", 0, null);
        }
        GroqException ultimo = null;
        for (String modelo : candidatos()) {
            try {
                GroqDTO.Respuesta r = enviar(modelo, mensajes);
                if (!modelo.equals(modeloActivo)) {
                    log.warn("Groq: el modelo {} no está disponible; se usa {}", modeloActivo, modelo);
                    modeloActivo = modelo;
                }
                return r;
            } catch (ModeloNoDisponible e) {
                ultimo = e.error;
            }
        }
        throw ultimo;
    }

    private GroqDTO.Respuesta enviar(String modelo, List<GroqDTO.Mensaje> mensajes) throws ModeloNoDisponible {
        try {
            String razonamiento = modelo.contains("gpt-oss") ? config.razonamiento() : null;
            String cuerpo = json.writeValueAsString(new GroqDTO.Peticion(
                    modelo, mensajes, config.temperatura(), config.maxTokens(), razonamiento));
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(config.urlBase() + "/chat/completions"))
                    .timeout(config.timeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> r = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() >= 400) {
                GroqException error = new GroqException(mensajeError(r.statusCode(), r.body()), r.statusCode(), null);
                if (modeloNoDisponible(r.statusCode(), r.body())) throw new ModeloNoDisponible(error);
                throw error;
            }
            return json.readValue(r.body(), GroqDTO.Respuesta.class);
        } catch (GroqException | ModeloNoDisponible e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GroqException("Se interrumpió la llamada a Groq", 0, e);
        } catch (Exception e) {
            throw new GroqException("No se pudo contactar a Groq: " + e.getClass().getSimpleName(), 0, e);
        }
    }

    private boolean modeloNoDisponible(int codigo, String cuerpo) {
        if (codigo == 404) return true;
        if (codigo != 400) return false;
        try {
            String tipo = json.readTree(cuerpo).path("error").path("code").asText("");
            return tipo.equals("model_not_found") || tipo.equals("model_decommissioned");
        } catch (Exception cuerpoNoJson) {
            return false;
        }
    }

    /** Mensaje en español según el código HTTP; incluye el detalle que devuelve Groq si lo hay. */
    private String mensajeError(int codigo, String cuerpo) {
        String detalle = "";
        try {
            JsonNode nodo = json.readTree(cuerpo).path("error").path("message");
            if (!nodo.isMissingNode()) detalle = " (" + nodo.asText() + ")";
        } catch (Exception cuerpoNoJson) {
            detalle = "";
        }
        String base = switch (codigo) {
            case 401, 403 -> "Groq rechazó la clave de API";
            case 404 -> "El modelo de Groq configurado no existe";
            case 429 -> "Se alcanzó el límite de uso de Groq; intente en unos segundos";
            default -> codigo >= 500 ? "Groq no está disponible en este momento" : "Groq rechazó la solicitud";
        };
        return base + detalle;
    }
}
