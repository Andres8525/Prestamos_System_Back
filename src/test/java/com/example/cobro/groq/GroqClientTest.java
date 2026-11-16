package com.example.cobro.groq;

import com.example.cobro.ia.NivelRiesgo;
import com.example.cobro.ia.ScoringDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Cliente de Groq contra un servidor HTTP falso que imita /chat/completions. */
class GroqClientTest {

    private HttpServer falso;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private volatile int codigo = 200;
    private volatile String respuesta = """
            {"id":"x","model":"llama-3.3-70b-versatile","choices":[{"index":0,
             "message":{"role":"assistant","content":"  Riesgo bajo: buen pagador.  "},"finish_reason":"stop"}],
             "usage":{"prompt_tokens":50,"completion_tokens":10,"total_tokens":60}}""";

    @BeforeEach
    void iniciar() throws IOException {
        falso = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        falso.createContext("/openai/v1/chat/completions", ex -> {
            cuerpoRecibido.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizacion.set(ex.getRequestHeaders().getFirst("Authorization"));
            byte[] b = respuesta.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(codigo, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        falso.start();
    }

    @AfterEach
    void detener() {
        falso.stop(0);
    }

    private GroqClient cliente(String clave) {
        String url = "http://127.0.0.1:" + falso.getAddress().getPort() + "/openai/v1/";
        return new GroqClient(new GroqProperties(clave, null, url, Duration.ofSeconds(5), 100, 0.2), new ObjectMapper());
    }

    @Test
    void enviaElFormatoCompatibleConOpenAIYLeeLaRespuesta() throws Exception {
        GroqDTO.Respuesta r = cliente("gsk_prueba").chat(List.of(GroqDTO.Mensaje.usuario("hola")));
        assertEquals("  Riesgo bajo: buen pagador.  ", r.texto());
        assertEquals(60, r.usage().totalTokens());
        assertEquals("Bearer gsk_prueba", autorizacion.get());

        Map<?, ?> enviado = new ObjectMapper().readValue(cuerpoRecibido.get(), Map.class);
        assertEquals(GroqProperties.MODELO_POR_DEFECTO, enviado.get("model"), "modelo por defecto");
        assertEquals("low", enviado.get("reasoning_effort"), "los modelos gpt-oss razonan con esfuerzo bajo");
        assertEquals(100, enviado.get("max_tokens"));
        assertEquals("user", ((Map<?, ?>) ((List<?>) enviado.get("messages")).get(0)).get("role"));
    }

    @Test
    void siGroqRetiraElModeloUsaElDeRespaldo() throws Exception {
        List<String> modelosPedidos = new java.util.concurrent.CopyOnWriteArrayList<>();
        falso.removeContext("/openai/v1/chat/completions");
        falso.createContext("/openai/v1/chat/completions", ex -> {
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String modelo = (String) new ObjectMapper().readValue(cuerpo, Map.class).get("model");
            modelosPedidos.add(modelo);
            boolean retirado = modelo.equals("llama-retirado");
            byte[] b = (retirado
                    ? "{\"error\":{\"message\":\"The model does not exist\",\"code\":\"model_not_found\"}}"
                    : respuesta).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(retirado ? 404 : 200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        String url = "http://127.0.0.1:" + falso.getAddress().getPort() + "/openai/v1";
        GroqClient groq = new GroqClient(new GroqProperties("gsk_prueba", "llama-retirado", List.of("respaldo-1"),
                url, Duration.ofSeconds(5), 100, 0.2, null), new ObjectMapper());

        assertEquals("  Riesgo bajo: buen pagador.  ", groq.chat(List.of(GroqDTO.Mensaje.usuario("hola"))).texto());
        assertEquals(GroqProperties.MODELO_POR_DEFECTO, groq.modelo());
        groq.chat(List.of(GroqDTO.Mensaje.usuario("otra")));
        assertEquals(List.of("llama-retirado", GroqProperties.MODELO_POR_DEFECTO, GroqProperties.MODELO_POR_DEFECTO), modelosPedidos,
                "después del cambio ya no se reintenta el modelo retirado");
    }

    @Test
    void elCatalogoDeGroqFijaElModeloAntesDeLaPrimeraPregunta() {
        falso.createContext("/openai/v1/models", ex -> {
            byte[] b = "{\"data\":[{\"id\":\"whisper\"},{\"id\":\"respaldo-1\"}]}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        String url = "http://127.0.0.1:" + falso.getAddress().getPort() + "/openai/v1";
        GroqClient groq = new GroqClient(new GroqProperties("gsk_prueba", "llama-retirado", List.of("respaldo-1"),
                url, Duration.ofSeconds(5), 100, 0.2, null), new ObjectMapper());
        assertEquals("llama-retirado", groq.modelo());
        groq.verificarModelo();
        assertEquals("respaldo-1", groq.modelo());
    }

    @Test
    void traduceLosErroresDeGroq() {
        codigo = 429;
        respuesta = "{\"error\":{\"message\":\"Rate limit reached\"}}";
        GroqClient.GroqException e = assertThrows(GroqClient.GroqException.class,
                () -> cliente("gsk_prueba").chat(List.of(GroqDTO.Mensaje.usuario("hola"))));
        assertEquals(429, e.getCodigoHttp());
        assertTrue(e.getMessage().contains("límite de uso"));
        assertTrue(e.getMessage().contains("Rate limit reached"));
    }

    @Test
    void sinClaveNoLlamaAGroq() {
        GroqClient sinClave = cliente("  ");
        assertFalse(sinClave.configurado());
        assertThrows(GroqClient.GroqException.class, () -> sinClave.chat(List.of()));
        assertNull(cuerpoRecibido.get());
    }

    @Test
    void losDatosQueSeEnvianNoIncluyenDatosPersonales() {
        ScoringDTO s = new ScoringDTO(7L, "Juan Pérez", 0.72, 280, NivelRiesgo.ALTO, "RECHAZAR", "msg", false, 2, 12,
                Map.of("promedioDiasAtraso", 14.0),
                List.of(new ScoringDTO.Factor("tasaSaldoVencido", "Proporción del saldo vencido sin pagar", 0.6, 1.2)),
                null);
        String datos = AsistenteIaService.datosScoring(s);
        assertFalse(datos.contains("Juan"));
        assertFalse(datos.contains("7L"));
        assertTrue(datos.contains("72.0 %"));
        assertTrue(AsistenteIaService.explicacionLocal(s).contains("proporción del saldo vencido sin pagar"));
    }

    @Test
    void elLimiteDeSolicitudesCortaAlSuperarElMaximo() {
        LimiteSolicitudesIa limite = new LimiteSolicitudesIa(2);
        assertTrue(limite.permitir(1L));
        assertTrue(limite.permitir(1L));
        assertFalse(limite.permitir(1L));
        assertTrue(limite.permitir(2L), "el límite es por usuario");
    }
}
