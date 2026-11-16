package com.example.cobro.groq;

import com.example.cobro.estructuras.IndiceAgrupado;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.ia.CreditScoringService;
import com.example.cobro.ia.ResumenModeloDTO;
import com.example.cobro.ia.ScoringDTO;
import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Pago;
import com.example.cobro.model.Prestamo;
import com.example.cobro.repository.PagoRepository;
import com.example.cobro.repository.PrestamoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Asistente de IA generativa (Groq) para el analista de crédito.
 *
 * <p><b>Privacidad:</b> a Groq solo se envían indicadores agregados o anónimos (variables de pago,
 * probabilidades, totales de cartera). Nunca nombres, cédulas, teléfonos ni direcciones.</p>
 *
 * <p>Si no hay {@code GROQ_API_KEY}, responde con un texto generado localmente a partir de los mismos datos,
 * de modo que la aplicación funciona igual sin la clave.</p>
 */
@Service
public class AsistenteIaService {

    static final int LARGO_MAXIMO_PREGUNTA = 600;
    static final String SISTEMA = """
            Eres el analista de riesgo de CrediRuta, una microfinanciera de préstamos con cobro diario en ruta.
            Responde siempre en español, en tono profesional y claro, en no más de 180 palabras.
            Usa solo los datos que se te entregan; si falta información dilo, no inventes cifras.
            No des asesoría legal. Cuando recomiendes acciones, que sean concretas para el cobrador o el analista.""";

    public enum Origen { GROQ, LOCAL }

    public record Respuesta(String texto, Origen origen, String modelo, Integer tokens) {}

    public record Estado(boolean configurado, String modelo, String proveedor) {}

    private final GroqClient groq;
    private final CreditScoringService scoringService;
    private final PrestamoRepository prestamoRepository;
    private final PagoRepository pagoRepository;

    public AsistenteIaService(GroqClient groq, CreditScoringService scoringService,
                              PrestamoRepository prestamoRepository, PagoRepository pagoRepository) {
        this.groq = groq;
        this.scoringService = scoringService;
        this.prestamoRepository = prestamoRepository;
        this.pagoRepository = pagoRepository;
    }

    public Estado estado() {
        return new Estado(groq.configurado(), groq.modelo(), "Groq");
    }

    /** Explica en lenguaje natural por qué el modelo de ML asignó ese riesgo a un cliente. */
    public Respuesta explicarCliente(Long clienteId) {
        ScoringDTO s = scoringService.evaluarCliente(clienteId);
        String datos = datosScoring(s);
        if (!groq.configurado()) {
            return new Respuesta(explicacionLocal(s), Origen.LOCAL, null, null);
        }
        return consultar("Explica al analista el riesgo de este cliente (datos anónimos) y sugiere 2 acciones:\n" + datos);
    }

    /** Pregunta libre sobre la cartera; se responde con el resumen agregado de la cartera como contexto. */
    public Respuesta preguntar(String pregunta) {
        String limpia = pregunta == null ? "" : pregunta.strip();
        if (limpia.isEmpty()) throw new ReglaNegocioException("Escriba una pregunta");
        if (limpia.length() > LARGO_MAXIMO_PREGUNTA) {
            throw new ReglaNegocioException("La pregunta no puede superar " + LARGO_MAXIMO_PREGUNTA + " caracteres");
        }
        String contexto = resumenCartera();
        if (!groq.configurado()) {
            return new Respuesta("El asistente con IA generativa no está activo (falta GROQ_API_KEY en el servidor). "
                    + "Resumen actual de la cartera:\n" + contexto, Origen.LOCAL, null, null);
        }
        return consultar("Datos actuales de la cartera:\n" + contexto + "\n\nPregunta del analista: " + limpia);
    }

    /** Informe ejecutivo de la cartera (lo usa el worker INFORME_IA). */
    public Respuesta informeCartera() {
        String contexto = resumenCartera();
        if (!groq.configurado()) {
            return new Respuesta("# Informe de cartera (sin IA generativa)\n\n" + contexto, Origen.LOCAL, null, null);
        }
        return consultar("Redacta un informe ejecutivo en Markdown (título, 3 hallazgos y 3 recomendaciones) "
                + "con estos datos de la cartera:\n" + contexto);
    }

    private Respuesta consultar(String prompt) {
        GroqDTO.Respuesta r = groq.chat(List.of(GroqDTO.Mensaje.sistema(SISTEMA), GroqDTO.Mensaje.usuario(prompt)));
        return new Respuesta(r.texto().strip(), Origen.GROQ, r.model() != null ? r.model() : groq.modelo(),
                r.usage() != null ? r.usage().totalTokens() : null);
    }

    static String datosScoring(ScoringDTO s) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "- Probabilidad de incumplimiento: %.1f %% (nivel %s, recomendación %s)%n",
                s.probabilidadDefault() * 100, s.nivelRiesgo(), s.recomendacion()));
        sb.append("- Préstamos del cliente: ").append(s.prestamosAnteriores())
          .append("; cuotas vencidas evaluadas: ").append(s.cuotasEvaluadas()).append('\n');
        if (s.caracteristicas() != null) {
            s.caracteristicas().forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append('\n'));
        }
        sb.append("- Factores que más pesan:\n");
        s.factores().forEach(f -> sb.append(String.format(Locale.ROOT, "  * %s (%s el riesgo)%n",
                f.descripcion(), f.impacto() > 0 ? "sube" : "baja")));
        if (s.modelo() != null) sb.append("- Modelo: ").append(s.modelo().algoritmo()).append('\n');
        return sb.toString();
    }

    static String explicacionLocal(ScoringDTO s) {
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT,
                "El modelo estima %.0f %% de probabilidad de incumplimiento (riesgo %s). %s",
                s.probabilidadDefault() * 100, s.nivelRiesgo().name().toLowerCase(Locale.ROOT), s.mensaje()));
        List<ScoringDTO.Factor> suben = s.factores().stream().filter(f -> f.impacto() > 0).toList();
        List<ScoringDTO.Factor> bajan = s.factores().stream().filter(f -> f.impacto() <= 0).toList();
        if (!suben.isEmpty()) {
            sb.append(" Lo que más aumenta el riesgo: ")
              .append(String.join(", ", suben.stream().map(f -> f.descripcion().toLowerCase(Locale.ROOT)).toList())).append('.');
        }
        if (!bajan.isEmpty()) {
            sb.append(" A su favor: ")
              .append(String.join(", ", bajan.stream().map(f -> f.descripcion().toLowerCase(Locale.ROOT)).toList())).append('.');
        }
        return sb.toString();
    }

    /** Indicadores agregados de la cartera (sin datos personales). */
    String resumenCartera() {
        LocalDate hoy = LocalDate.now();
        Map<EstadoPago, Integer> porEstado = new EnumMap<>(EstadoPago.class);
        Map<String, Integer> porRiesgo = new java.util.TreeMap<>();
        double saldo = 0, vencido = 0;
        int activos = 0;
        List<Prestamo> prestamos = prestamoRepository.findAll();
        IndiceAgrupado<Long, Pago> pendientes = IndiceAgrupado.de(
                pagoRepository.findByPrestamo_IdInAndFechaPagoIsNullOrderByPrestamo_IdAscNumeroAsc(
                        prestamos.stream().filter(p -> p.getEstado() != EstadoPago.PAGADO).map(Prestamo::getId).toList()),
                Pago::getPrestamoId);
        for (Prestamo p : prestamos) {
            porEstado.merge(p.getEstado(), 1, Integer::sum);
            if (p.getEstado() == EstadoPago.PAGADO) continue;
            activos++;
            porRiesgo.merge(p.getNivelRiesgo() != null ? p.getNivelRiesgo() : "SIN_EVALUAR", 1, Integer::sum);
            for (Pago c : pendientes.de(p.getId())) {
                saldo += c.saldo();
                if (c.getFechaProgramada().isBefore(hoy)) vencido += c.saldo();
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("- Préstamos activos: ").append(activos).append('\n');
        sb.append("- Préstamos por estado: ").append(porEstado).append('\n');
        sb.append("- Riesgo asignado al aprobar (activos): ").append(porRiesgo).append('\n');
        sb.append(String.format(Locale.ROOT, "- Saldo por cobrar: %.2f; saldo vencido: %.2f (%.1f %%)%n",
                saldo, vencido, saldo > 0 ? vencido * 100 / saldo : 0));
        try {
            ResumenModeloDTO m = scoringService.getResumen();
            sb.append(String.format(Locale.ROOT, "- Modelo de ML en uso: %s, AUC en prueba %.3f%n",
                    m.modelo().algoritmo(), m.evaluacionPrueba().auc()));
        } catch (Exception modeloNoEntrenado) {
            sb.append("- Modelo de ML: aún no entrenado").append('\n');
        }
        return sb.toString();
    }
}
