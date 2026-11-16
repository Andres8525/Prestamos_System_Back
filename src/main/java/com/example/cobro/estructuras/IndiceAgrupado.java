package com.example.cobro.estructuras;

import java.util.*;
import java.util.function.Function;

/**
 * Índice "multimapa": agrupa una lista por clave en una sola pasada (O(n)) y responde cada consulta en O(1).
 * Sustituye el patrón N+1 de hacer una consulta a la base por cada préstamo.
 * Dentro de cada grupo se conserva el orden de la lista original.
 */
public final class IndiceAgrupado<K, V> {

    private final Map<K, List<V>> grupos;

    private IndiceAgrupado(Map<K, List<V>> grupos) {
        this.grupos = grupos;
    }

    public static <K, V> IndiceAgrupado<K, V> de(Collection<V> elementos, Function<V, K> clave) {
        Map<K, List<V>> grupos = new HashMap<>();
        for (V elemento : elementos) {
            grupos.computeIfAbsent(clave.apply(elemento), k -> new ArrayList<>()).add(elemento);
        }
        return new IndiceAgrupado<>(grupos);
    }

    /** Elementos de la clave (lista vacía e inmodificable si no hay ninguno). */
    public List<V> de(K clave) {
        return Collections.unmodifiableList(grupos.getOrDefault(clave, List.of()));
    }

    public Set<K> claves() {
        return Collections.unmodifiableSet(grupos.keySet());
    }

    public int cantidadGrupos() {
        return grupos.size();
    }
}
