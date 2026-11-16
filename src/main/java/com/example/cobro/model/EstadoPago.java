package com.example.cobro.model;

public enum EstadoPago {
    AL_DIA,
    PENDIENTE,
    ATRASADO,
    /** Préstamo totalmente cancelado (todas sus cuotas pagadas). */
    PAGADO
}
