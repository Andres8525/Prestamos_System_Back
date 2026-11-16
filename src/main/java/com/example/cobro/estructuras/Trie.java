package com.example.cobro.estructuras;

import java.text.Normalizer;
import java.util.*;

/**
 * Árbol de prefijos (Trie) para autocompletar. Cada palabra indexada lleva asociado un identificador;
 * buscar un prefijo de longitud m cuesta O(m) más los resultados recorridos.
 * Las palabras se normalizan: minúsculas y sin tildes ("Pérez" se encuentra con "pere").
 */
public class Trie<T> {

    private static final class Nodo<T> {
        private final Map<Character, Nodo<T>> hijos = new TreeMap<>();
        private final Set<T> valores = new LinkedHashSet<>();
    }

    private final Nodo<T> raiz = new Nodo<>();
    private int palabras;

    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }

    /** Indexa cada palabra del texto (separadas por espacios) asociándola al valor. */
    public void insertar(String texto, T valor) {
        for (String palabra : normalizar(texto).split("\\s+")) {
            if (palabra.isEmpty()) continue;
            Nodo<T> nodo = raiz;
            for (char c : palabra.toCharArray()) {
                nodo = nodo.hijos.computeIfAbsent(c, k -> new Nodo<>());
            }
            nodo.valores.add(valor);
            palabras++;
        }
    }

    /** Valores cuyas palabras empiezan con el prefijo, sin repetir, hasta {@code limite}. */
    public List<T> buscar(String prefijo, int limite) {
        String p = normalizar(prefijo);
        if (p.isEmpty() || limite <= 0) return List.of();
        Nodo<T> nodo = raiz;
        for (char c : p.toCharArray()) {
            nodo = nodo.hijos.get(c);
            if (nodo == null) return List.of();
        }
        Set<T> encontrados = new LinkedHashSet<>();
        Deque<Nodo<T>> pila = new ArrayDeque<>();
        pila.push(nodo);
        while (!pila.isEmpty() && encontrados.size() < limite) {
            Nodo<T> actual = pila.pop();
            for (T v : actual.valores) {
                if (encontrados.size() == limite) break;
                encontrados.add(v);
            }
            List<Nodo<T>> hijos = new ArrayList<>(actual.hijos.values());
            Collections.reverse(hijos);
            hijos.forEach(pila::push);
        }
        return new ArrayList<>(encontrados);
    }

    public int cantidadPalabras() {
        return palabras;
    }
}
