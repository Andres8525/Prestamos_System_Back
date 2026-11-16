package com.example.cobro.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256 y sal aleatoria por usuario.
 * Formato almacenado: {@code pbkdf2$iteraciones$salBase64$hashBase64}.
 */
@Component
public class PasswordHasher {

    private static final int ITERACIONES = 120_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] sal = new byte[BYTES_SAL];
        random.nextBytes(sal);
        byte[] hash = derivar(password, sal, ITERACIONES);
        return "pbkdf2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public boolean coincide(String password, String almacenado) {
        if (password == null || almacenado == null) return false;
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) return false;
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            // Comparación en tiempo constante para no filtrar información por tiempos de respuesta.
            return MessageDigest.isEqual(esperado, derivar(password, sal, iteraciones));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derivar(String password, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, iteraciones, BITS_HASH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }
}
