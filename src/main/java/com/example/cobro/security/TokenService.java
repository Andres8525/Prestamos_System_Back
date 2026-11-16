package com.example.cobro.security;

import com.example.cobro.model.Administrador;
import com.example.cobro.model.ParametroSistema;
import com.example.cobro.repository.ParametroSistemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Emite y valida tokens de sesión en formato JWT (HS256).
 *
 * <p>La clave de firma se toma de JWT_SECRET. Si no está definida se genera una aleatoria
 * <b>una sola vez</b> y se guarda en la base de datos, de modo que las sesiones siguen siendo
 * válidas aunque el servidor se reinicie (en Render Free el servicio se duerme y despierta a menudo).</p>
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    static final String PARAMETRO_CLAVE = "jwt.secret";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String CABECERA = B64.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper mapper = new ObjectMapper();
    private final String secretoConfigurado;
    private final long minutosVigencia;
    private final ParametroSistemaRepository parametros;
    private byte[] clave;

    public TokenService(@Value("${app.jwt.secret:}") String secreto,
                        @Value("${app.jwt.expiration-minutes:480}") long minutosVigencia,
                        ParametroSistemaRepository parametros) {
        this.secretoConfigurado = secreto;
        this.minutosVigencia = minutosVigencia;
        this.parametros = parametros;
    }

    @PostConstruct
    void inicializarClave() {
        if (secretoConfigurado != null && !secretoConfigurado.isBlank()) {
            if (secretoConfigurado.length() < 32) {
                log.warn("JWT_SECRET tiene menos de 32 caracteres; use una clave más larga.");
            }
            clave = secretoConfigurado.getBytes(StandardCharsets.UTF_8);
            return;
        }
        clave = parametros.findById(PARAMETRO_CLAVE)
                .map(p -> Base64.getDecoder().decode(p.getValor()))
                .orElseGet(this::generarYGuardarClave);
    }

    private byte[] generarYGuardarClave() {
        byte[] nueva = new byte[48];
        new SecureRandom().nextBytes(nueva);
        parametros.save(new ParametroSistema(PARAMETRO_CLAVE, Base64.getEncoder().encodeToString(nueva)));
        log.info("JWT_SECRET no está definido: se generó una clave de sesión y se guardó en la base de datos.");
        return nueva;
    }

    /** Datos de la sesión contenidos en un token válido. */
    public record Sesion(Long usuarioId, String email, String nombre, Instant expira) {}

    public long getSegundosVigencia() {
        return minutosVigencia * 60;
    }

    public String emitir(Administrador usuario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("sub", usuario.getId());
        datos.put("email", usuario.getEmail());
        datos.put("nombre", usuario.getNombre());
        datos.put("exp", Instant.now().getEpochSecond() + getSegundosVigencia());
        try {
            String cuerpo = CABECERA + "." + B64.encodeToString(mapper.writeValueAsBytes(datos));
            return cuerpo + "." + firmar(cuerpo);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo emitir el token", e);
        }
    }

    /** Devuelve la sesión si el token tiene firma válida y no ha expirado. */
    public Optional<Sesion> validar(String token) {
        if (token == null) return Optional.empty();
        String[] partes = token.split("\\.");
        if (partes.length != 3) return Optional.empty();
        try {
            String esperada = firmar(partes[0] + "." + partes[1]);
            if (!MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8),
                    partes[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }
            Map<?, ?> datos = mapper.readValue(B64D.decode(partes[1]), Map.class);
            Instant expira = Instant.ofEpochSecond(((Number) datos.get("exp")).longValue());
            if (Instant.now().isAfter(expira)) return Optional.empty();
            return Optional.of(new Sesion(((Number) datos.get("sub")).longValue(),
                    (String) datos.get("email"), (String) datos.get("nombre"), expira));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String firmar(String contenido) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(clave, "HmacSHA256"));
        return B64.encodeToString(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
    }
}
