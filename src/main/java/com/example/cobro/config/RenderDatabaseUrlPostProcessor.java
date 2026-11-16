package com.example.cobro.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Permite usar en DATABASE_URL la URL que entrega Render
 * ({@code postgresql://usuario:clave@host[:puerto]/base}) convirtiéndola al formato JDBC
 * ({@code jdbc:postgresql://host:puerto/base}) y extrayendo usuario y contraseña.
 * Si DATABASE_URL ya es JDBC no se modifica. Solo actúa con el perfil {@code prod}.
 */
public class RenderDatabaseUrlPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String FUENTE = "renderDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (!Arrays.asList(env.getActiveProfiles()).contains("prod")) return;
        String url = env.getProperty("DATABASE_URL");
        if (url == null || url.isBlank()) return;

        Map<String, Object> props = convertir(url.trim());
        if (!props.isEmpty()) {
            env.getPropertySources().addFirst(new MapPropertySource(FUENTE, props));
        }
    }

    /** Devuelve las propiedades spring.datasource.* equivalentes; vacío si la URL ya es JDBC. */
    static Map<String, Object> convertir(String url) {
        Map<String, Object> props = new HashMap<>();
        if (!url.startsWith("postgres://") && !url.startsWith("postgresql://")) return props;

        URI uri = URI.create(url);
        int puerto = uri.getPort() > 0 ? uri.getPort() : 5432;
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost()).append(':').append(puerto).append(uri.getPath());

        String query = uri.getQuery();
        // Las conexiones externas de Render (host *.render.com) exigen SSL; la interna no.
        if ((query == null || !query.contains("sslmode")) && uri.getHost().endsWith(".render.com")) {
            query = (query == null ? "" : query + "&") + "sslmode=require";
        }
        if (query != null && !query.isEmpty()) jdbc.append('?').append(query);
        props.put("spring.datasource.url", jdbc.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            String[] partes = userInfo.split(":", 2);
            props.put("spring.datasource.username", decodificar(partes[0]));
            if (partes.length > 1) props.put("spring.datasource.password", decodificar(partes[1]));
        }
        return props;
    }

    private static String decodificar(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        // Después de cargar application*.properties y los archivos importados (.env).
        return Ordered.LOWEST_PRECEDENCE;
    }
}
