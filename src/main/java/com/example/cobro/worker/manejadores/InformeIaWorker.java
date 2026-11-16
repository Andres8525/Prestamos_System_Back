package com.example.cobro.worker.manejadores;

import com.example.cobro.groq.AsistenteIaService;
import com.example.cobro.worker.ManejadorTarea;
import com.example.cobro.worker.TipoTarea;
import org.springframework.stereotype.Component;

/**
 * Redacta un informe ejecutivo de la cartera con Groq en segundo plano (la llamada al modelo de lenguaje
 * puede tardar varios segundos). Sin GROQ_API_KEY genera el informe con los indicadores locales.
 * Si Groq falla (límite de uso, caída), la excepción hace que el pool reintente la tarea.
 */
@Component
public class InformeIaWorker implements ManejadorTarea {

    private final AsistenteIaService asistente;

    public InformeIaWorker(AsistenteIaService asistente) {
        this.asistente = asistente;
    }

    @Override
    public TipoTarea tipo() {
        return TipoTarea.INFORME_IA;
    }

    @Override
    public Resultado ejecutar() {
        AsistenteIaService.Respuesta r = asistente.informeCartera();
        String resumen = r.origen() == AsistenteIaService.Origen.GROQ
                ? "Informe generado con Groq (" + r.modelo() + (r.tokens() != null ? ", " + r.tokens() + " tokens" : "") + ")"
                : "Informe generado localmente (Groq no configurado)";
        return new Resultado(resumen, r.texto());
    }
}
