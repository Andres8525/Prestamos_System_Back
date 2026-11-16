package com.example.cobro.groq;

import com.example.cobro.security.AuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

/** API del asistente de IA generativa (Groq). La clave de Groq nunca sale del servidor. */
@RestController
@RequestMapping("/api/asistente")
public class AsistenteController {

    private final AsistenteIaService asistente;
    private final LimiteSolicitudesIa limite;

    public AsistenteController(AsistenteIaService asistente, LimiteSolicitudesIa limite) {
        this.asistente = asistente;
        this.limite = limite;
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
    public AsistenteIaService.Respuesta explicarCliente(@PathVariable Long clienteId, HttpServletRequest request) {
        verificarLimite(request);
        return asistente.explicarCliente(clienteId);
    }

    @PostMapping("/preguntar")
    public AsistenteIaService.Respuesta preguntar(@Valid @RequestBody Pregunta p, HttpServletRequest request) {
        verificarLimite(request);
        return asistente.preguntar(p.pregunta());
    }

    private void verificarLimite(HttpServletRequest request) {
        if (!asistente.estado().configurado()) return; // el modo local no consume cuota
        Long usuario = (Long) request.getAttribute(AuthFilter.ATRIBUTO_USUARIO_ID);
        if (!limite.permitir(usuario)) {
            throw new GroqClient.GroqException("Demasiadas consultas al asistente; espere un minuto", 429, null);
        }
    }
}
