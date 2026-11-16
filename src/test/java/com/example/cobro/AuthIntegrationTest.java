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
}
