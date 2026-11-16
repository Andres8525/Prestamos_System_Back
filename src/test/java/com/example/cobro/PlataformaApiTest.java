package com.example.cobro;

import com.example.cobro.security.AuthFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** API de disponibilidad, workers, gRPC (info) y asistente de IA, con autenticación real. */
@SpringBootTest
class PlataformaApiTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private AuthFilter authFilter;
    @Autowired private ObjectMapper json;
    private MockMvc mvc;
    private String token;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(authFilter).build();
        String cuerpo = json.writeValueAsString(Map.of("nombre", "Plataforma", "email",
                "plataforma" + System.nanoTime() + "@test.com", "password", "Segura123"));
        String r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        token = "Bearer " + json.readTree(r).get("token").asText();
    }

    @Test
    void healthYPingSonPublicosEInformanLaBase() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.baseDatos").value("UP"));
        mvc.perform(get("/api/ping")).andExpect(status().isOk()).andExpect(jsonPath("$.pong").exists());
    }

    @Test
    void laApiDeWorkersEncolaYPermiteDescargarElReporte() throws Exception {
        mvc.perform(get("/api/workers/estado")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/workers/estado").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hilos").value(2))
                .andExpect(jsonPath("$.tipos.length()").value(5));

        String creada = mvc.perform(post("/api/workers/tareas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"REPORTE_CARTERA\",\"prioridad\":9}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.solicitadaPor").exists())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).get("id").asLong();

        JsonNode tarea = null;
        for (int i = 0; i < 200; i++) {
            tarea = json.readTree(mvc.perform(get("/api/workers/tareas/" + id).header("Authorization", token))
                    .andReturn().getResponse().getContentAsString());
            if ("COMPLETADA".equals(tarea.get("estado").asText())) break;
            Thread.sleep(100);
        }
        assertEquals("COMPLETADA", tarea.get("estado").asText());
        assertTrue(tarea.get("tieneResultado").asBoolean());
        mvc.perform(get("/api/workers/tareas/" + id + "/resultado").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")));

        mvc.perform(post("/api/workers/tareas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"NO_EXISTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elAsistenteFuncionaEnModoLocalSinClaveDeGroq() throws Exception {
        mvc.perform(get("/api/asistente/estado").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurado").value(false))
                .andExpect(jsonPath("$.proveedor").value("Groq"));
        mvc.perform(post("/api/asistente/preguntar").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pregunta\":\"¿Cómo está la cartera?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("LOCAL"))
                .andExpect(jsonPath("$.texto").value(org.hamcrest.Matchers.containsString("Préstamos activos")));
        mvc.perform(post("/api/asistente/preguntar").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pregunta\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laInformacionGrpcListaLosServiciosYSusPatrones() throws Exception {
        mvc.perform(get("/api/grpc/info").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.servicios[?(@.nombre=='crediruta.v1.ScoringService')].metodos.length()").value(3));
        mvc.perform(get("/api/grpc/contratos").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['cobranza.proto']").value(org.hamcrest.Matchers.containsString("SincronizarCobros")));
    }
}
