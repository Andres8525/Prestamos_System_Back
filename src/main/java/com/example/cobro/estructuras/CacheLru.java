package com.example.cobro.estructuras;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Caché LRU (Least Recently Used) de capacidad fija, segura entre hilos.
 *
 * <p>Se apoya en {@link LinkedHashMap} con orden de acceso: cada lectura mueve la entrada al final y,
 * al superar la capacidad, se descarta la del principio (la menos usada). Lectura y escritura en O(1).</p>
 */
public class CacheLru<K, V> {

    private final int capacidad;
    private final LinkedHashMap<K, V> entradas;
    private long aciertos;
    private long fallos;

    public CacheLru(int capacidad) {
        if (capacidad <= 0) throw new IllegalArgumentException("La capacidad debe ser positiva");
        this.capacidad = capacidad;
        this.entradas = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> mayor) {
                return size() > CacheLru.this.capacidad;
            }
        };
    }

    public synchronized Optional<V> obtener(K clave) {
        V valor = entradas.get(clave);
        if (valor == null) fallos++; else aciertos++;
        return Optional.ofNullable(valor);
    }

    /** Devuelve el valor guardado o lo calcula, lo guarda y lo devuelve. */
    public V obtenerOCalcular(K clave, Function<K, V> calculo) {
        Optional<V> guardado = obtener(clave);
        if (guardado.isPresent()) return guardado.get();
        V valor = calculo.apply(clave);
        guardar(clave, valor);
        return valor;
    }

    public synchronized void guardar(K clave, V valor) {
        entradas.put(clave, valor);
    }

    public synchronized void invalidar(K clave) {
        entradas.remove(clave);
    }

    public synchronized void limpiar() {
        entradas.clear();
    }

    public synchronized int tamano() {
        return entradas.size();
    }

    public synchronized double tasaAciertos() {
        long total = aciertos + fallos;
        return total == 0 ? 0 : (double) aciertos / total;
    }
}
