package com.example.cobro.grpc;

import com.example.cobro.dto.RutaCobroDTO;
import com.example.cobro.grpc.v1.*;
import com.example.cobro.service.RutaCobroService;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Ruta de cobro y registro de abonos por gRPC (cliente previsto: la app móvil del cobrador). */
@Component
public class CobranzaGrpcService extends CobranzaServiceGrpc.CobranzaServiceImplBase {

    private final RutaCobroService rutaCobroService;

    public CobranzaGrpcService(RutaCobroService rutaCobroService) {
        this.rutaCobroService = rutaCobroService;
    }

    /** Streaming de servidor: una {@link Parada} por mensaje, en orden de visita. */
    @Override
    public void obtenerRuta(RutaRequest peticion, StreamObserver<Parada> respuesta) {
        ServerCallStreamObserver<Parada> stream = (ServerCallStreamObserver<Parada>) respuesta;
        try {
            LocalDate fecha = fecha(peticion.getFecha());
            RutaCobroDTO ruta = rutaCobroService.generarRuta(peticion.getCobradorId(), fecha);
            for (RutaCobroDTO.Parada p : ruta.paradas()) {
                if (stream.isCancelled()) return;
                stream.onNext(Parada.newBuilder()
                        .setOrden(p.orden())
                        .setClienteId(p.clienteId())
                        .setClienteNombre(nulo(p.clienteNombre()))
                        .setDireccion(nulo(p.direccion()))
                        .setPrestamoId(p.prestamoId())
                        .setValorCuota(p.valorCuota())
                        .setMontoExigible(p.montoExigible())
                        .setCuotasVencidas(p.cuotasVencidas())
                        .setDiasAtraso(p.diasAtraso())
                        .setSaldoPendiente(p.saldoPendiente())
                        .setAbonadoHoy(p.abonadoHoy())
                        .setCobraHoy(p.cobraHoy())
                        .setEstado(p.estado() != null ? p.estado().name() : "")
                        .build());
            }
            stream.onCompleted();
        } catch (Exception e) {
            stream.onError(ErroresGrpc.traducir(e));
        }
    }

    static LocalDate fecha(String iso) {
        if (iso == null || iso.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(iso.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida (use AAAA-MM-DD): " + iso);
        }
    }

    private static String nulo(String valor) {
        return valor == null ? "" : valor;
    }
}
