package com.example.cobro.worker;

/** Trabajos en segundo plano que pueden encolarse para los workers. */
public enum TipoTarea {
    RECALCULAR_MORA("Recalcular mora de la cartera", "Actualiza el estado (al día, pendiente, atrasado) de cada préstamo activo."),
    SCORING_CARTERA("Scoring de toda la cartera", "Evalúa con el modelo de ML a cada cliente con crédito activo y guarda la predicción."),
    REENTRENAR_MODELO("Reentrenar modelo de ML", "Vuelve a entrenar y comparar los algoritmos con los préstamos que ya tienen desenlace."),
    REPORTE_CARTERA("Reporte de cartera (CSV)", "Genera un CSV con saldo, atraso y riesgo de cada préstamo activo."),
    INFORME_IA("Informe de cartera con IA (Groq)", "Redacta un resumen ejecutivo de la cartera con un modelo de lenguaje (Groq).");

    private final String nombre;
    private final String descripcion;

    TipoTarea(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
