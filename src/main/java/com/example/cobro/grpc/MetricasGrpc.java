package com.example.cobro.grpc;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Contadores por método gRPC: llamadas, errores y tiempo acumulado. */
@Component
public class MetricasGrpc {

    public static final class Metodo {
        private final LongAdder llamadas = new LongAdder();
        private final LongAdder errores = new LongAdder();
        private final LongAdder milisegundos = new LongAdder();
        private volatile String ultimoEstado = "-";

        void registrar(boolean ok, long ms, String estado) {
            llamadas.increment();
            if (!ok) errores.increment();
            milisegundos.add(ms);
            ultimoEstado = estado;
        }

        public long llamadas() { return llamadas.sum(); }
        public long errores() { return errores.sum(); }
        public double promedioMs() {
            long n = llamadas.sum();
            return n == 0 ? 0 : (double) milisegundos.sum() / n;
        }
        public String ultimoEstado() { return ultimoEstado; }
    }

    private final Map<String, Metodo> porMetodo = new ConcurrentHashMap<>();

    public void registrar(String metodoCompleto, boolean ok, long ms, String estado) {
        porMetodo.computeIfAbsent(metodoCompleto, k -> new Metodo()).registrar(ok, ms, estado);
    }

    public Metodo de(String metodoCompleto) {
        return porMetodo.getOrDefault(metodoCompleto, new Metodo());
    }
}
