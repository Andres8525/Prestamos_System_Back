package com.example.cobro.worker;

import com.example.cobro.model.TareaProceso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorkerPoolTest {

    @Autowired private TareaService tareas;
    @Autowired private WorkerPool pool;

    private TareaProceso esperarFin(long id) throws InterruptedException {
        long limite = System.currentTimeMillis() + 30_000;
        TareaProceso t = tareas.obtener(id);
        while (!t.getEstado().terminal() && System.currentTimeMillis() < limite) {
            Thread.sleep(100);
            t = tareas.obtener(id);
        }
        return t;
    }

    @Test
    void losWorkersProcesanLaColaYGuardanElResultado() throws Exception {
        assertTrue(pool.isRunning());
        TareaProceso mora = tareas.encolar(TipoTarea.RECALCULAR_MORA, 5, "test");
        TareaProceso reporte = tareas.encolar(TipoTarea.REPORTE_CARTERA, 9, "test");

        TareaProceso r = esperarFin(reporte.getId());
        assertEquals(EstadoTarea.COMPLETADA, r.getEstado());
        assertTrue(r.getResultado().startsWith("prestamo_id,cliente"));
        assertTrue(r.getWorker().startsWith("worker-"));
        assertEquals(1, r.getIntentos());

        TareaProceso m = esperarFin(mora.getId());
        assertEquals(EstadoTarea.COMPLETADA, m.getEstado());
        assertTrue(m.getResumen().contains("préstamos revisados"));
        assertNotNull(m.duracionMs());
    }

    @Test
    void soloSeCancelanPendientesYSoloSeReintentanFallidas() throws Exception {
        TareaProceso t = tareas.encolar(TipoTarea.RECALCULAR_MORA, 0, "test");
        TareaProceso fin = esperarFin(t.getId());
        assertThrows(RuntimeException.class, () -> tareas.cancelar(fin.getId()));
        assertThrows(RuntimeException.class, () -> tareas.reintentar(fin.getId()));
    }

    @Test
    void laColaAtiendePrimeroLaMayorPrioridadYLuegoElOrdenDeLlegada() throws Exception {
        ColaTareas cola = new ColaTareas();
        cola.encolar(1, 1);
        cola.encolar(2, 9);
        cola.encolar(3, 9);
        cola.encolar(4, 5);
        assertEquals(2L, cola.tomar(10));
        assertEquals(3L, cola.tomar(10));
        assertEquals(4L, cola.tomar(10));
        assertTrue(cola.quitar(1));
        assertNull(cola.tomar(10));
    }

    @Test
    void elReporteEscapaComasYComillas() {
        assertEquals("\"Pérez, Ana\"", com.example.cobro.worker.manejadores.ReporteCarteraWorkerAcceso.texto("Pérez, Ana"));
        assertEquals("\"Dice \"\"hola\"\"\"", com.example.cobro.worker.manejadores.ReporteCarteraWorkerAcceso.texto("Dice \"hola\""));
    }
}
