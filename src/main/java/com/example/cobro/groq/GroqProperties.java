package com.example.cobro.groq;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración de la API de Groq (modelos de lenguaje con API compatible con OpenAI).
 * La clave se define SOLO en el backend ({@code GROQ_API_KEY}); nunca se envía al frontend.
 *
 * @param apiKey      clave de https://console.groq.com/keys (vacía = asistente desactivado)
 * @param modelo      modelo de chat a usar
 * @param urlBase     endpoint compatible con OpenAI
 * @param timeout     tiempo máximo de espera por respuesta
 * @param maxTokens   límite de tokens de la respuesta
 * @param temperatura creatividad (0 = determinista)
 */
@ConfigurationProperties(prefix = "app.groq")
public record GroqProperties(String apiKey, String modelo, String urlBase, Duration timeout,
                             Integer maxTokens, Double temperatura) {

    public GroqProperties {
        apiKey = apiKey == null ? "" : apiKey.trim();
        modelo = blanco(modelo) ? "llama-3.3-70b-versatile" : modelo.trim();
        urlBase = blanco(urlBase) ? "https://api.groq.com/openai/v1" : urlBase.trim().replaceAll("/+$", "");
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        maxTokens = maxTokens == null ? 700 : maxTokens;
        temperatura = temperatura == null ? 0.3 : temperatura;
    }

    public boolean configurado() {
        return !apiKey.isEmpty();
    }

    private static boolean blanco(String s) {
        return s == null || s.isBlank();
    }
}
