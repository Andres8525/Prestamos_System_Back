package com.example.cobro.grpc;

import com.example.cobro.exception.NoAutorizadoException;
import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.exception.RiesgoCreditoException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Traduce las excepciones de negocio a códigos de estado gRPC
 * (el equivalente del GlobalExceptionHandler de la API REST).
 */
public final class ErroresGrpc {

    private ErroresGrpc() {}

    public static StatusRuntimeException traducir(Throwable e) {
        if (e instanceof StatusRuntimeException sre) return sre;
        Status estado;
        if (e instanceof RecursoNoEncontradoException) estado = Status.NOT_FOUND;
        else if (e instanceof NoAutorizadoException) estado = Status.UNAUTHENTICATED;
        else if (e instanceof RiesgoCreditoException) estado = Status.FAILED_PRECONDITION;
        else if (e instanceof ReglaNegocioException || e instanceof IllegalArgumentException) estado = Status.INVALID_ARGUMENT;
        else estado = Status.INTERNAL;
        String mensaje = estado == Status.INTERNAL ? "Error interno del servidor" : e.getMessage();
        return estado.withDescription(mensaje).withCause(e).asRuntimeException();
    }

    /** Mensaje legible de un error para incluirlo en una respuesta (sin cortar el stream). */
    public static String mensaje(Throwable e) {
        StatusRuntimeException sre = traducir(e);
        return sre.getStatus().getDescription() != null ? sre.getStatus().getDescription() : sre.getStatus().getCode().name();
    }
}
