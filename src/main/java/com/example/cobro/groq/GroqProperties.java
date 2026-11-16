package com.example.cobro.groq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;
import java.util.List;

/**
 * Configuración de la API de Groq (modelos de lenguaje con API compatible con OpenAI).
 * La clave se define SOLO en el backend ({@code GROQ_API_KEY}); nunca se envía al frontend.
 *
 * @param apiKey          clave de https://console.groq.com/keys (vacía = asistente desactivado)
 * @param modelo          modelo de chat preferido
 * @param modelosRespaldo modelos que se prueban, en orden, si Groq retira o no ofrece el preferido
 * @param urlBase         endpoint compatible con OpenAI
 * @param timeout         tiempo máximo de espera por respuesta
 * @param maxTokens       límite de tokens de la respuesta
 * @param temperatura     creatividad (0 = determinista)
 * @param razonamiento    esfuerzo de razonamiento de los modelos gpt-oss (low, medium, high)
 */
@ConfigurationProperties(prefix = "app.groq")
public record GroqProperties(String apiKey, String modelo, List<String> modelosRespaldo, String urlBase,
                             Duration timeout, Integer maxTokens, Double temperatura, String razonamiento) {

    public static final String MODELO_POR_DEFECTO = "openai/gpt-oss-120b";
    public static final List<String> RESPALDO_POR_DEFECTO = List.of("openai/gpt-oss-20b", "qwen/qwen3.8-27b");

    @ConstructorBinding
    public GroqProperties {
        apiKey = apiKey == null ? "" : apiKey.trim();
        modelo = blanco(modelo) ? MODELO_POR_DEFECTO : modelo.trim();
        modelosRespaldo = modelosRespaldo == null || modelosRespaldo.isEmpty() ? RESPALDO_POR_DEFECTO
                : modelosRespaldo.stream().filter(m -> !blanco(m)).map(String::trim).toList();
        urlBase = blanco(urlBase) ? "https://api.groq.com/openai/v1" : urlBase.trim().replaceAll("/+$", "");
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        maxTokens = maxTokens == null ? 700 : maxTokens;
        temperatura = temperatura == null ? 0.3 : temperatura;
        razonamiento = blanco(razonamiento) ? "low" : razonamiento.trim();
    }

    public GroqProperties(String apiKey, String modelo, String urlBase, Duration timeout,
                          Integer maxTokens, Double temperatura) {
        this(apiKey, modelo, null, urlBase, timeout, maxTokens, temperatura, null);
    }

    public boolean configurado() {
        return !apiKey.isEmpty();
    }

    private static boolean blanco(String s) {
        return s == null || s.isBlank();
    }
}
