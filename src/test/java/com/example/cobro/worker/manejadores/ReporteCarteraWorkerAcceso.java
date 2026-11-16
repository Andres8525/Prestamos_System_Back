package com.example.cobro.worker.manejadores;

/** Expone a las pruebas de otro paquete el escape CSV (método de paquete). */
public final class ReporteCarteraWorkerAcceso {
    private ReporteCarteraWorkerAcceso() {}

    public static String texto(String valor) {
        return ReporteCarteraWorker.texto(valor);
    }
}
