package com.example.cobro.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Cliente HTTP mínimo de la API de chat de Groq (sin SDK: java.net.http + Jackson). */
@Component
public class GroqClient {

    /** Error al llamar a Groq (clave inválida, límite de uso, caída del servicio, tiempo agotado…). */
    public static class GroqException extends RuntimeException {
        private final int codigoHttp;

        public GroqException(String mensaje, int codigoHttp, Throwable causa) {
            super(mensaje, causa);
            this.codigoHttp = codigoHttp;
        }

        public int getCodigoHttp() { return codigoHttp; }
    }

    private final GroqProperties config;
    private final ObjectMapper json;
    private final HttpClient http;

    public GroqClient(GroqProperties config, ObjectMapper json) {
        this.config = config;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(config.timeout()).build();
    }

    public boolean configurado() {
        return config.configurado();
    }

    public String modelo() {
        return config.modelo();
    }

    public GroqDTO.Respuesta chat(List<GroqDTO.Mensaje> mensajes) {
        if (!configurado()) {
            throw new GroqException("La API de Groq no está configurada (defina GROQ_API_KEY en el backend)", 0, null);
        }
        try {
            String cuerpo = json.writeValueAsString(new GroqDTO.Peticion(
                    config.modelo(), mensajes, config.temperatura(), config.maxTokens()));
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(config.urlBase() + "/chat/completions"))
                    .timeout(config.timeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> r = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() >= 400) {
                throw new GroqException(mensajeError(r.statusCode(), r.body()), r.statusCode(), null);
            }
            return json.readValue(r.body(), GroqDTO.Respuesta.class);
        } catch (GroqException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GroqException("Se interrumpió la llamada a Groq", 0, e);
        } catch (Exception e) {
            throw new GroqException("No se pudo contactar a Groq: " + e.getClass().getSimpleName(), 0, e);
        }
    }

    /** Mensaje en español según el código HTTP; incluye el detalle que devuelve Groq si lo hay. */
    private String mensajeError(int codigo, String cuerpo) {
        String detalle = "";
        try {
            JsonNode nodo = json.readTree(cuerpo).path("error").path("message");
            if (!nodo.isMissingNode()) detalle = " (" + nodo.asText() + ")";
        } catch (Exception ignorado) {
            // cuerpo no JSON
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
