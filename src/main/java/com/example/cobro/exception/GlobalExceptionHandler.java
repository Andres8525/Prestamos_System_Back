package com.example.cobro.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de la aplicación a respuestas JSON homogéneas.
 * Formato de error simple: {"error": "mensaje"}.
 * Formato de validación: {"campo": "mensaje", ...}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, String>> reglaNegocio(ReglaNegocioException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String campo = err instanceof FieldError fe ? fe.getField() : err.getObjectName();
            errores.put(campo, err.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + ex.getName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT,
                "La operación viola la integridad de los datos (registro duplicado o con dependencias)");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> generico(Exception ex) {
        log.error("Error no controlado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(Map.of("error", mensaje != null ? mensaje : status.getReasonPhrase()));
    }
}
