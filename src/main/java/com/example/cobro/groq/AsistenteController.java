package com.example.cobro.groq;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

/** API del asistente de IA generativa (Groq). La clave de Groq nunca sale del servidor. */
@RestController
@RequestMapping("/api/asistente")
public class AsistenteController {

    private final AsistenteIaService asistente;

    public AsistenteController(AsistenteIaService asistente) {
        this.asistente = asistente;
    }

    public record Pregunta(@NotBlank(message = "Escriba una pregunta")
                           @Size(max = AsistenteIaService.LARGO_MAXIMO_PREGUNTA,
                                 message = "La pregunta es demasiado larga") String pregunta) {}

    /** ¿Está configurada la API de Groq? (el frontend muestra el asistente en modo local si no). */
    @GetMapping("/estado")
    public AsistenteIaService.Estado estado() {
        return asistente.estado();
    }

    @PostMapping("/explicar/cliente/{clienteId}")
    public AsistenteIaService.Respuesta explicarCliente(@PathVariable Long clienteId) {
        return asistente.explicarCliente(clienteId);
    }

    @PostMapping("/preguntar")
    public AsistenteIaService.Respuesta preguntar(@Valid @RequestBody Pregunta p) {
        return asistente.preguntar(p.pregunta());
    }
}
