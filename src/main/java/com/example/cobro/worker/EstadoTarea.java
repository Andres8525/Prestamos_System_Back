package com.example.cobro.worker;

/** Ciclo de vida de una tarea: PENDIENTE → EN_PROCESO → COMPLETADA | FALLIDA (o CANCELADA si nunca empezó). */
public enum EstadoTarea {
    PENDIENTE,
    EN_PROCESO,
    COMPLETADA,
    FALLIDA,
    CANCELADA;

    public boolean terminal() {
        return this == COMPLETADA || this == FALLIDA || this == CANCELADA;
    }
}
