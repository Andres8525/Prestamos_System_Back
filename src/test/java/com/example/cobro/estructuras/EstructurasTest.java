package com.example.cobro.estructuras;

import com.example.cobro.dto.RutaCobroDTO;
import com.example.cobro.tiemporeal.EventoTiempoReal;
import com.example.cobro.tiemporeal.HistorialEventos;
import com.example.cobro.tiemporeal.TipoEvento;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstructurasTest {

    @Test
    void laCacheLruDescartaLaMenosUsada() {
        CacheLru<String, Integer> cache = new CacheLru<>(2);
        cache.guardar("a", 1);
        cache.guardar("b", 2);
        cache.obtener("a");
        cache.guardar("c", 3);
        assertTrue(cache.obtener("a").isPresent());
        assertTrue(cache.obtener("b").isEmpty(), "b era la menos usada");
        assertEquals(2, cache.tamano());
        assertEquals(3, cache.obtenerOCalcular("c", k -> 99));
        assertEquals(0.75, cache.tasaAciertos(), 1e-9);
    }

    @Test
    void elIndiceAgrupaConservandoElOrden() {
        IndiceAgrupado<Integer, String> indice = IndiceAgrupado.de(List.of("a1", "b1", "a2", "c1"), s -> (int) s.charAt(0));
        assertEquals(List.of("a1", "a2"), indice.de((int) 'a'));
        assertEquals(List.of(), indice.de((int) 'z'));
        assertEquals(3, indice.cantidadGrupos());
    }

    @Test
    void elTrieEncuentraPorPrefijoSinTildesYSinRepetir() {
        Trie<Long> trie = new Trie<>();
        trie.insertar("María Pérez", 1L);
        trie.insertar("0912345678", 1L);
        trie.insertar("Mario Peña", 2L);
        trie.insertar("Pedro Ruiz", 3L);
        assertEquals(List.of(1L, 2L), trie.buscar("mar", 10));
        assertEquals(List.of(3L, 2L, 1L), trie.buscar("PE", 10), "orden alfabético: pedro, pena, perez");
        assertEquals(List.of(1L), trie.buscar("perez", 10));
        assertEquals(List.of(1L), trie.buscar("0912", 10));
        assertEquals(1, trie.buscar("pe", 1).size());
        assertTrue(trie.buscar("x", 5).isEmpty());
        assertTrue(trie.buscar("  ", 5).isEmpty());
    }

    @Test
    void laColaDePrioridadOrdenaPorUrgencia() {
        List<RutaCobroDTO.Parada> ordenadas = PrioridadCobro.ordenar(List.of(
                parada(1, false, 0, 0), parada(2, true, 2, 30), parada(3, true, 10, 10), parada(4, true, 2, 50)));
        assertEquals(List.of(3, 4, 2, 1), ordenadas.stream().map(p -> p.clienteId().intValue()).toList());
        assertEquals(List.of(1, 2, 3, 4), ordenadas.stream().map(RutaCobroDTO.Parada::orden).toList());
    }

    @Test
    void losTramosDeMoraClasificanYAcumulan() {
        TramosMora tramos = new TramosMora();
        assertEquals("Al día", tramos.tramoDe(-3).etiqueta());
        assertEquals("1-7 días", tramos.tramoDe(7).etiqueta());
        assertEquals("16-30 días", tramos.tramoDe(16).etiqueta());
        assertEquals("Más de 60 días", tramos.tramoDe(400).etiqueta());
        List<TramosMora.Resumen> r = tramos.resumir(List.of(new long[]{3, 1050}, new long[]{5, 950}, new long[]{90, 100}));
        assertEquals(6, r.size());
        assertEquals(new TramosMora.Resumen("1-7 días", 2, 20.0), r.get(1));
        assertEquals(1, r.get(5).cuotas());
    }

    @Test
    void elHistorialCircularGuardaSoloLosUltimos() {
        HistorialEventos historial = new HistorialEventos(3);
        for (long i = 1; i <= 5; i++) {
            historial.agregar(new EventoTiempoReal(i, TipoEvento.ABONO_REGISTRADO, "e" + i, Map.of(), Instant.now()));
        }
        assertEquals(3, historial.tamano());
        assertEquals(List.of(4L, 5L), historial.desde(3).stream().map(EventoTiempoReal::id).toList());
        assertEquals(List.of(5L), historial.recientes(1).stream().map(EventoTiempoReal::id).toList());
    }

    private static RutaCobroDTO.Parada parada(long cliente, boolean cobraHoy, long atraso, double exigible) {
        return new RutaCobroDTO.Parada((int) cliente, cliente, "C" + cliente, "", "", cliente, null, null,
                10, exigible, 0, atraso, 100, 0, cobraHoy);
    }
}
