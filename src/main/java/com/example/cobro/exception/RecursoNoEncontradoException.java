package com.example.cobro.exception;

/** Se lanza cuando una entidad solicitada no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException de(String recurso, Long id) {
        return new RecursoNoEncontradoException(recurso + " no encontrado con ID: " + id);
    }
}
