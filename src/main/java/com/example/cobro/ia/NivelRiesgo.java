package com.example.cobro.ia;

/** Clasificación del riesgo de incumplimiento y la acción sugerida al prestamista. */
public enum NivelRiesgo {

    BAJO("APROBAR", "Buen historial de pagos. Se puede renovar el crédito."),
    MEDIO("REVISAR", "Historial irregular. Renovar con monto reducido, plazo menor o garantía."),
    ALTO("RECHAZAR", "Alta probabilidad de incumplimiento. No se recomienda renovar el crédito.");

    public static final double UMBRAL_MEDIO = 0.30;
    public static final double UMBRAL_ALTO = 0.60;

    private final String recomendacion;
    private final String mensaje;

    NivelRiesgo(String recomendacion, String mensaje) {
        this.recomendacion = recomendacion;
        this.mensaje = mensaje;
    }

    public static NivelRiesgo desdeProbabilidad(double probabilidad) {
        if (probabilidad >= UMBRAL_ALTO) return ALTO;
        if (probabilidad >= UMBRAL_MEDIO) return MEDIO;
        return BAJO;
    }

    public String getRecomendacion() { return recomendacion; }
    public String getMensaje() { return mensaje; }
}
