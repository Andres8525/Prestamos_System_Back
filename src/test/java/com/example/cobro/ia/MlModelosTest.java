package com.example.cobro.ia;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MlModelosTest {

    private final List<Muestra> entrenamiento = CreditScoringService.generarMuestrasSinteticas(800, 3L);
    private final List<Muestra> prueba = CreditScoringService.generarMuestrasSinteticas(300, 77L);

    @Test
    void elArbolDeDecisionAprendeYExplicaSusPredicciones() {
        ArbolDecision arbol = new ArbolDecision();
        arbol.entrenar(entrenamiento);

        assertTrue(arbol.auc(prueba) > 0.8, "AUC del árbol en datos no vistos");
        assertTrue(arbol.contarHojas() > 1 && arbol.contarHojas() <= 16, "profundidad máxima 4 => hasta 16 hojas");

        double[] imp = arbol.importancia();
        assertEquals(1.0, java.util.Arrays.stream(imp).sum(), 1e-9);

        double[] moroso = {20, 45, 0.9, 0.8, 0.3, 0.7, 0};
        double[] buenPagador = {0, 0, 0, 0, 1.0, 0, 4};
        assertTrue(arbol.predecir(moroso) > arbol.predecir(buenPagador));
        // Las contribuciones del camino suman la diferencia entre la hoja y la raíz.
        assertTrue(java.util.Arrays.stream(arbol.contribuciones(moroso)).sum() > 0);
    }

    @Test
    void lasMetricasCalculanMatrizDeConfusionYAuc() {
        ModeloClasificacion fijo = new ModeloClasificacion() {
            public String nombre() { return "fijo"; }
            public void entrenar(List<Muestra> m) {}
            public double predecir(double[] x) { return x[0]; }
            public double[] contribuciones(double[] x) { return x; }
            public double[] importancia() { return new double[]{1}; }
            public boolean estaEntrenado() { return true; }
        };
        List<Muestra> datos = List.of(
                new Muestra(new double[]{0.9}, 1, 1), new Muestra(new double[]{0.8}, 1, 1),
                new Muestra(new double[]{0.3}, 1, 1), new Muestra(new double[]{0.6}, 0, 1),
                new Muestra(new double[]{0.2}, 0, 1), new Muestra(new double[]{0.1}, 0, 1));

        MetricasClasificacion.Resultado r = MetricasClasificacion.evaluar(fijo, datos);
        assertEquals(new MetricasClasificacion.MatrizConfusion(2, 1, 2, 1), r.matriz());
        assertEquals(0.667, r.exactitud());
        assertEquals(0.667, r.precision());
        assertEquals(0.667, r.sensibilidad());
        assertEquals(0.889, r.auc()); // 8 de 9 pares moroso/buen pagador bien ordenados
        assertEquals(21, r.curvaRoc().size());
        assertEquals(1.0, r.curvaRoc().get(20).tpr());
    }

    @Test
    void laDivisionYLaValidacionCruzadaSonEstratificadas() {
        ValidacionCruzada.Division d = ValidacionCruzada.dividir(entrenamiento, 0.2, 1L);
        assertEquals(entrenamiento.size(), d.entrenamiento().size() + d.prueba().size());
        double proporcionTotal = proporcionPositivos(entrenamiento);
        assertEquals(proporcionTotal, proporcionPositivos(d.prueba()), 0.02);

        ValidacionCruzada.Resultado cv = ValidacionCruzada.evaluar(RegresionLogistica::new, entrenamiento, 5, 1L);
        assertEquals(5, cv.pliegues());
        assertTrue(cv.aucMedia() > 0.8);
        assertTrue(cv.aucDesviacion() < 0.1, "el desempeño debe ser estable entre pliegues");
    }

    @Test
    void ambosAlgoritmosCandidatosSuperanElAzar() {
        for (var fabrica : CreditScoringService.candidatos()) {
            ModeloClasificacion m = fabrica.get();
            m.entrenar(new ArrayList<>(entrenamiento));
            assertTrue(m.auc(prueba) > 0.75, m.nombre() + " debe superar ampliamente a un modelo al azar (0.5)");
        }
    }

    private static double proporcionPositivos(List<Muestra> muestras) {
        return muestras.stream().filter(m -> m.y() == 1).count() / (double) muestras.size();
    }
}
