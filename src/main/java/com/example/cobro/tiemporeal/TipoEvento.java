package com.example.cobro.tiemporeal;

/** Eventos que el servidor envía por WebSocket. Los de sistema no se guardan en el historial. */
public enum TipoEvento {
    BIENVENIDA(true),
    LATIDO(true),
    CONEXIONES(true),
    ABONO_REGISTRADO(false),
    PRESTAMO_CREADO(false),
    CAJA_CERRADA(false),
    TAREA_ACTUALIZADA(false),
    MODELO_ENTRENADO(false);

    private final boolean sistema;

    TipoEvento(boolean sistema) {
        this.sistema = sistema;
    }

    public boolean esSistema() {
        return sistema;
    }
}
