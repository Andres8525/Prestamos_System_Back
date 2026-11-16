package com.example.cobro.exception;

/** Credenciales incorrectas o sesión inválida. Se traduce a HTTP 401. */
public class NoAutorizadoException extends RuntimeException {

    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
