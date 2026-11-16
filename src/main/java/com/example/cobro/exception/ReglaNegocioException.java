package com.example.cobro.exception;

/** Se lanza cuando una operación viola una regla del negocio. Se traduce a HTTP 400. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
