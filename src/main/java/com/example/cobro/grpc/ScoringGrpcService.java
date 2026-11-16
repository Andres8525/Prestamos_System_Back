package com.example.cobro.grpc;

import com.example.cobro.grpc.v1.*;
import com.example.cobro.ia.CaracteristicasCrediticias;
import com.example.cobro.ia.CreditScoringService;
import com.example.cobro.ia.ScoringDTO;
import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Prestamo;
import com.example.cobro.repository.PrestamoRepository;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/** Implementación gRPC del scoring de riesgo: reutiliza el mismo servicio de ML que la API REST. */
@Component
public class ScoringGrpcService extends ScoringServiceGrpc.ScoringServiceImplBase {

    private final CreditScoringService scoringService;
    private final PrestamoRepository prestamoRepository;

    public ScoringGrpcService(CreditScoringService scoringService, PrestamoRepository prestamoRepository) {
        this.scoringService = scoringService;
        this.prestamoRepository = prestamoRepository;
    }

    /** Unario: una petición, una respuesta. */
    @Override
    public void evaluarCliente(EvaluarClienteRequest peticion, StreamObserver<EvaluacionRiesgo> respuesta) {
        try {
            respuesta.onNext(aMensaje(scoringService.evaluarCliente(peticion.getClienteId())));
            respuesta.onCompleted();
        } catch (Exception e) {
            respuesta.onError(ErroresGrpc.traducir(e));
        }
    }

    /** Unario: evalúa un comportamiento de pago hipotético. */
    @Override
    public void simular(CaracteristicasPago c, StreamObserver<EvaluacionRiesgo> respuesta) {
        try {
            validar(c);
            CaracteristicasCrediticias caracteristicas = new CaracteristicasCrediticias(
                    c.getPromedioDiasAtraso(), Math.max(c.getMaxDiasAtraso(), c.getPromedioDiasAtraso()),
                    c.getTasaCuotasAtrasadas(), c.getTasaAbonosIncompletos(), c.getFrecuenciaPago(),
                    c.getTasaSaldoVencido(), c.getPrestamosPagados(), 10);
            respuesta.onNext(aMensaje(scoringService.evaluar(null, 0, caracteristicas)));
            respuesta.onCompleted();
        } catch (Exception e) {
            respuesta.onError(ErroresGrpc.traducir(e));
        }
    }

    /**
     * Streaming de servidor: envía la evaluación de cada cliente con crédito activo a medida que se calcula.
     * Si el cliente cancela la llamada, se deja de evaluar.
     */
    @Override
    public void evaluarCartera(EvaluarCarteraRequest peticion, StreamObserver<EvaluacionRiesgo> respuesta) {
        ServerCallStreamObserver<EvaluacionRiesgo> stream = (ServerCallStreamObserver<EvaluacionRiesgo>) respuesta;
        try {
            Set<Long> clientes = new LinkedHashSet<>();
            prestamoRepository.findAll().stream()
                    .filter(p -> p.getEstado() != EstadoPago.PAGADO)
                    .map(Prestamo::getClienteId)
                    .forEach(clientes::add);
            int minimo = peticion.getNivelMinimo().getNumber();
            for (Long id : clientes) {
                if (stream.isCancelled()) return;
                EvaluacionRiesgo e = aMensaje(scoringService.evaluarCliente(id));
                if (e.getNivel().getNumber() >= minimo) stream.onNext(e);
            }
            stream.onCompleted();
        } catch (Exception e) {
            stream.onError(ErroresGrpc.traducir(e));
        }
    }

    private static void validar(CaracteristicasPago c) {
        if (c.getPromedioDiasAtraso() < 0 || c.getMaxDiasAtraso() < 0 || c.getPrestamosPagados() < 0) {
            throw new IllegalArgumentException("Los días de atraso y los préstamos pagados no pueden ser negativos");
        }
        for (double tasa : new double[]{c.getTasaCuotasAtrasadas(), c.getTasaAbonosIncompletos(), c.getTasaSaldoVencido()}) {
            if (tasa < 0 || tasa > 1) throw new IllegalArgumentException("Las tasas deben estar entre 0 y 1");
        }
    }

    static EvaluacionRiesgo aMensaje(ScoringDTO s) {
        EvaluacionRiesgo.Builder b = EvaluacionRiesgo.newBuilder()
                .setProbabilidadDefault(s.probabilidadDefault())
                .setPuntaje(s.puntaje())
                .setNivel(nivel(s.nivelRiesgo()))
                .setRecomendacion(s.recomendacion());
        if (s.clienteId() != null) b.setClienteId(s.clienteId());
        if (s.clienteNombre() != null) b.setClienteNombre(s.clienteNombre());
        if (s.modelo() != null) b.setAlgoritmo(s.modelo().algoritmo());
        s.factores().forEach(f -> b.addFactores(Factor.newBuilder()
                .setVariable(f.variable()).setDescripcion(f.descripcion()).setImpacto(f.impacto())));
        return b.build();
    }

    static NivelRiesgo nivel(com.example.cobro.ia.NivelRiesgo nivel) {
        return NivelRiesgo.valueOf("NIVEL_RIESGO_" + nivel.name());
    }
}
