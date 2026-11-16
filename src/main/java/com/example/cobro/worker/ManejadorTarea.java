package com.example.cobro.worker;

/**
 * Estrategia que ejecuta un {@link TipoTarea}. Cada implementación es un bean de Spring;
 * el pool de workers la localiza por su tipo (patrón Strategy + registro).
 */
public interface ManejadorTarea {

    TipoTarea tipo();

    /**
     * Ejecuta el trabajo. Una excepción marca el intento como fallido y la tarea se reintenta
     * hasta su máximo de intentos.
     */
    Resultado ejecutar() throws Exception;

    /**
     * @param resumen   texto corto para la lista de tareas
     * @param contenido contenido generado (CSV, informe); puede ser null
     */
    record Resultado(String resumen, String contenido) {
        public static Resultado de(String resumen) {
            return new Resultado(resumen, null);
        }
    }
}
