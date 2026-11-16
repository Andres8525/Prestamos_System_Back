package com.example.cobro;

import com.example.cobro.repository.AdministradorRepository;
import com.example.cobro.security.PasswordHasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.example.cobro.security.AuthFilter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthIntegrationTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private AuthFilter authFilter;
    private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private AdministradorRepository administradorRepository;
    @Autowired private PasswordHasher passwordHasher;
    @Autowired private com.example.cobro.repository.ParametroSistemaRepository parametros;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(authFilter).build();
    }

    @Test
    void laApiExigeTokenYElRegistroPermiteIniciarSesion() throws Exception {
        mvc.perform(get("/api/cobradores")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/cobradores").header("Authorization", "Bearer token.falso.xyz"))
                .andExpect(status().isUnauthorized());

        String registro = json.writeValueAsString(Map.of(
                "nombre", "Ana Pruebas", "email", "Ana@Test.com", "password", "Segura123"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.email").value("ana@test.com"));
        // El email no puede repetirse y la contraseña se guarda como hash.
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isBadRequest());
        String guardada = administradorRepository.findByEmail("ana@test.com").orElseThrow().getContrasena();
        assertNotEquals("Segura123", guardada);
        assertTrue(passwordHasher.coincide("Segura123", guardada));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "ana@test.com", "password", "incorrecta1"))))
                .andExpect(status().isUnauthorized());

        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "ana@test.com", "password", "Segura123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(respuesta).get("token").asText();

        mvc.perform(get("/api/cobradores").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Pruebas"));
        // Un token manipulado se rechaza.
        mvc.perform(get("/api/cobradores").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroRechazaContrasenasDebiles() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nombre", "X", "email", "debil@test.com", "password", "corta"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void elAccesoEnDosPasosIndicaSiElEmailTieneCuenta() throws Exception {
        mvc.perform(post("/api/auth/verificar-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "nuevo@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "nombre", "Nuevo", "email", "nuevo@test.com", "password", "Clave1234"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/verificar-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "  NUEVO@test.com "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.email").value("nuevo@test.com"));

        mvc.perform(post("/api/auth/verificar-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "no-es-un-email"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laClaveDeSesionSeGuardaEnLaBaseDeDatos() {
        assertTrue(parametros.findById("jwt.secret").isPresent(),
                "sin JWT_SECRET la clave se persiste para sobrevivir a los reinicios");
    }

    @Test
    void laApiDeMachineLearningEntregaElReporteYElSimulador() throws Exception {
        String registro = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nombre", "Analista", "email", "ml@test.com", "password", "Modelo2024"))))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(registro).get("token").asText();

        mvc.perform(get("/api/ml/modelo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparacion.length()").value(2))
                .andExpect(jsonPath("$.evaluacionPrueba.curvaRoc.length()").value(21))
                .andExpect(jsonPath("$.validacionCruzada.pliegues").value(5));

        mvc.perform(post("/api/ml/simular").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "promedioDiasAtraso", 20, "maxDiasAtraso", 45, "tasaCuotasAtrasadas", 0.9,
                                "tasaAbonosIncompletos", 0.8, "frecuenciaPago", 0.3, "tasaSaldoVencido", 0.7,
                                "prestamosPagados", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelRiesgo").value("ALTO"));

        mvc.perform(get("/api/ml/entrenamientos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNotEmpty());
    }
}
