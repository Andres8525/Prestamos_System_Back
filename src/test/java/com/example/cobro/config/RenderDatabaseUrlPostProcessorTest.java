package com.example.cobro.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RenderDatabaseUrlPostProcessorTest {

    @Test
    void convierteLaUrlInternaDeRenderAJdbc() {
        Map<String, Object> p = RenderDatabaseUrlPostProcessor.convertir(
                "postgresql://usuario:cl%40ve@dpg-abc123-a/mi_base");
        assertEquals("jdbc:postgresql://dpg-abc123-a:5432/mi_base", p.get("spring.datasource.url"));
        assertEquals("usuario", p.get("spring.datasource.username"));
        assertEquals("cl@ve", p.get("spring.datasource.password"));
    }

    @Test
    void laUrlExternaUsaSsl() {
        Map<String, Object> p = RenderDatabaseUrlPostProcessor.convertir(
                "postgres://u:p@dpg-abc123-a.oregon-postgres.render.com:5432/db");
        assertEquals("jdbc:postgresql://dpg-abc123-a.oregon-postgres.render.com:5432/db?sslmode=require",
                p.get("spring.datasource.url"));
    }

    @Test
    void unaUrlJdbcNoSeModifica() {
        assertTrue(RenderDatabaseUrlPostProcessor.convertir("jdbc:postgresql://host:5432/db").isEmpty());
    }
}
