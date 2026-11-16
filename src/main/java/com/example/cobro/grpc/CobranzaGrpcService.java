package com.example.cobro.grpc;

import com.example.cobro.dto.AbonoResponseDTO;
import com.example.cobro.dto.RutaCobroDTO;
import com.example.cobro.grpc.v1.*;
import com.example.cobro.service.AbonoService;
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
    private final AbonoService abonoService;

    public CobranzaGrpcService(RutaCobroService rutaCobroService, AbonoService abonoService) {
        this.rutaCobroService = rutaCobroService;
        this.abonoService = abonoService;
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

    /**
     * Streaming de cliente: el cobrador envía todos los abonos de la jornada (por ejemplo, los que tomó
     * sin señal) y al cerrar el stream recibe un resumen. Un abono rechazado no detiene a los demás.
     */
    @Override
    public StreamObserver<AbonoRequest> registrarAbonos(StreamObserver<ResumenAbonos> respuesta) {
        return new StreamObserver<>() {
            private final ResumenAbonos.Builder resumen = ResumenAbonos.newBuilder();
            private double total;

            @Override
            public void onNext(AbonoRequest abono) {
                ConfirmacionAbono c = registrar(abono);
                resumen.addDetalle(c).setRecibidos(resumen.getRecibidos() + 1);
                if (c.getAceptado()) {
                    resumen.setAceptados(resumen.getAceptados() + 1);
                    total += abono.getMonto();
                } else {
                    resumen.setRechazados(resumen.getRechazados() + 1);
                }
            }

            @Override
            public void onError(Throwable t) {
                // El cliente canceló: los abonos ya registrados se conservan.
            }

            @Override
            public void onCompleted() {
                respuesta.onNext(resumen.setTotalRegistrado(Math.round(total * 100) / 100.0).build());
                respuesta.onCompleted();
            }
        };
    }

    /**
     * Bidireccional: cada abono que llega se registra y se confirma de inmediato por el mismo canal,
     * así la app muestra el saldo actualizado sin esperar al final de la jornada.
     */
    @Override
    public StreamObserver<AbonoRequest> sincronizarCobros(StreamObserver<ConfirmacionAbono> respuesta) {
        return new StreamObserver<>() {
            @Override
            public void onNext(AbonoRequest abono) {
                respuesta.onNext(registrar(abono));
            }

            @Override
            public void onError(Throwable t) {
                // Conexión perdida: el cliente reenviará los abonos sin confirmación.
            }

            @Override
            public void onCompleted() {
                respuesta.onCompleted();
            }
        };
    }

    /** Registra un abono y devuelve su confirmación; los errores de negocio se informan sin cortar el stream. */
    ConfirmacionAbono registrar(AbonoRequest abono) {
        ConfirmacionAbono.Builder c = ConfirmacionAbono.newBuilder().setIdLocal(abono.getIdLocal());
        try {
            if (abono.getMonto() <= 0) throw new IllegalArgumentException("El monto del abono debe ser mayor que cero");
            AbonoResponseDTO r = abonoService.registrarAbono(abono.getPrestamoId(), abono.getMonto(),
                    abono.getCobradorId() > 0 ? abono.getCobradorId() : null,
                    abono.getObservacion().isBlank() ? null : abono.getObservacion());
            c.setAceptado(true).setAbonoId(r.getId());
            if (r.getSaldoPrestamo() != null) c.setSaldoPrestamo(r.getSaldoPrestamo());
            if (r.getCuotasCubiertas() != null) c.setCuotasCubiertas(r.getCuotasCubiertas());
        } catch (Exception e) {
            c.setAceptado(false).setError(ErroresGrpc.mensaje(e));
        }
        return c.build();
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
