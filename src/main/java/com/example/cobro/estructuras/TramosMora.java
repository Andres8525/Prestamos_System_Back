package com.example.cobro.estructuras;

import java.util.*;

/**
 * Clasifica días de atraso en tramos (al día, 1-7, 8-15, 16-30, 31-60, más de 60) con un {@link TreeMap}:
 * {@code floorEntry} encuentra el tramo de cualquier valor en O(log k) sin cadenas de if.
 */
public final class TramosMora {

    public record Tramo(String etiqueta, int desde, Integer hasta) {}

    public record Resumen(String tramo, int cuotas, double saldo) {}

    private final NavigableMap<Integer, Tramo> tramos = new TreeMap<>();

    public TramosMora() {
        agregar("Al día", 0, 0);
        agregar("1-7 días", 1, 7);
        agregar("8-15 días", 8, 15);
        agregar("16-30 días", 16, 30);
        agregar("31-60 días", 31, 60);
        agregar("Más de 60 días", 61, null);
    }

    private void agregar(String etiqueta, int desde, Integer hasta) {
        tramos.put(desde, new Tramo(etiqueta, desde, hasta));
    }

    public Tramo tramoDe(long diasAtraso) {
        return tramos.floorEntry((int) Math.max(0, Math.min(diasAtraso, Integer.MAX_VALUE))).getValue();
    }

    public List<Tramo> tramos() {
        return List.copyOf(tramos.values());
    }

    /** Acumula cuotas y saldo por tramo; incluye los tramos vacíos para graficar siempre los mismos. */
    public List<Resumen> resumir(Collection<long[]> diasYSaldoEnCentavos) {
        Map<String, int[]> cuotas = new LinkedHashMap<>();
        Map<String, Long> centavos = new LinkedHashMap<>();
        tramos.values().forEach(t -> {
            cuotas.put(t.etiqueta(), new int[1]);
            centavos.put(t.etiqueta(), 0L);
        });
        for (long[] par : diasYSaldoEnCentavos) {
            String etiqueta = tramoDe(par[0]).etiqueta();
            cuotas.get(etiqueta)[0]++;
            centavos.merge(etiqueta, par[1], Long::sum);
        }
        List<Resumen> resultado = new ArrayList<>();
        cuotas.forEach((etiqueta, n) -> resultado.add(new Resumen(etiqueta, n[0], centavos.get(etiqueta) / 100.0)));
        return resultado;
    }
}
