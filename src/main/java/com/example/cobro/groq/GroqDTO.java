package com.example.cobro.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Mensajes de la API de chat de Groq (formato compatible con OpenAI /chat/completions). */
public final class GroqDTO {

    private GroqDTO() {}

    public record Mensaje(String role, String content) {
        public static Mensaje sistema(String texto) { return new Mensaje("system", texto); }
        public static Mensaje usuario(String texto) { return new Mensaje("user", texto); }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Peticion(String model, List<Mensaje> messages, Double temperature,
                           @JsonProperty("max_tokens") Integer maxTokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Respuesta(String model, List<Opcion> choices, Uso usage) {
        public String texto() {
            return choices == null || choices.isEmpty() || choices.get(0).message() == null
                    ? "" : choices.get(0).message().content();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Opcion(Mensaje message, @JsonProperty("finish_reason") String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Uso(@JsonProperty("prompt_tokens") int promptTokens,
                      @JsonProperty("completion_tokens") int completionTokens,
                      @JsonProperty("total_tokens") int totalTokens) {}
}
