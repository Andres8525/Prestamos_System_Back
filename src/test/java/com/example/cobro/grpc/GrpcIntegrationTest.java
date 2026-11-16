package com.example.cobro.grpc;

import com.example.cobro.dto.AuthDTO;
import com.example.cobro.dto.CobradorDTO;
import com.example.cobro.dto.PrestamoRequestDTO;
import com.example.cobro.grpc.v1.*;
import com.example.cobro.model.Cliente;
import com.example.cobro.model.Cobrador;
import com.example.cobro.model.Prestamo;
import com.example.cobro.model.TipoLiquidacion;
import com.example.cobro.service.AuthService;
import com.example.cobro.service.ClienteService;
import com.example.cobro.service.CobradorService;
import com.example.cobro.service.PrestamoService;
import io.grpc.*;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba los cuatro patrones de gRPC contra el servidor real (puerto aleatorio). */
@SpringBootTest
class GrpcIntegrationTest {

    @Autowired private GrpcServidor servidor;
    @Autowired private AuthService authService;
    @Autowired private CobradorService cobradorService;
    @Autowired private ClienteService clienteService;
    @Autowired private PrestamoService prestamoService;
    @Autowired private GrpcInfoController info;

    private ManagedChannel canal;
    private Channel autenticado;

    @BeforeEach
    void conectar() {
        assertTrue(servidor.isRunning());
        canal = ManagedChannelBuilder.forAddress("localhost", servidor.getPuerto()).usePlaintext().build();
        String email = "grpc" + System.nanoTime() + "@test.com";
        String token = authService.registrar(new AuthDTO.Registro("Prueba gRPC", email, "Segura123")).token();
        Metadata cabeceras = new Metadata();
        cabeceras.put(AutenticacionGrpcInterceptor.AUTORIZACION, "Bearer " + token);
        autenticado = ClientInterceptors.intercept(canal, MetadataUtils.newAttachHeadersInterceptor(cabeceras));
    }

    @AfterEach
    void cerrar() throws InterruptedException {
        canal.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void healthEsPublicoYLosServiciosExigenToken() {
        HealthCheckResponse salud = HealthGrpc.newBlockingStub(canal).check(HealthCheckRequest.getDefaultInstance());
        assertEquals(HealthCheckResponse.ServingStatus.SERVING, salud.getStatus());

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> ScoringServiceGrpc.newBlockingStub(canal).simular(CaracteristicasPago.getDefaultInstance()));
        assertEquals(Status.Code.UNAUTHENTICATED, e.getStatus().getCode());
    }

    @Test
    void unarioSimulaYValidaLosArgumentos() {
        var stub = ScoringServiceGrpc.newBlockingStub(autenticado);
        EvaluacionRiesgo moroso = stub.simular(CaracteristicasPago.newBuilder()
                .setPromedioDiasAtraso(18).setMaxDiasAtraso(45).setTasaCuotasAtrasadas(0.85)
                .setTasaAbonosIncompletos(0.75).setFrecuenciaPago(0.3).setTasaSaldoVencido(0.65).build());
        assertEquals(NivelRiesgo.NIVEL_RIESGO_ALTO, moroso.getNivel());
        assertFalse(moroso.getAlgoritmo().isBlank());
        assertFalse(moroso.getFactoresList().isEmpty());

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> stub.simular(CaracteristicasPago.newBuilder().setTasaSaldoVencido(3).build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());

        StatusRuntimeException noExiste = assertThrows(StatusRuntimeException.class,
                () -> stub.evaluarCliente(EvaluarClienteRequest.newBuilder().setClienteId(987654).build()));
        assertEquals(Status.Code.NOT_FOUND, noExiste.getStatus().getCode());
    }

    @Test
    void streamingDeRutaDeClienteYBidireccional() throws Exception {
        Cobrador cobrador = nuevoCobrador();
        Prestamo prestamo = nuevoPrestamo(cobrador);

        // Streaming de servidor
        Iterator<Parada> paradas = CobranzaServiceGrpc.newBlockingStub(autenticado)
                .obtenerRuta(RutaRequest.newBuilder().setCobradorId(cobrador.getId()).build());
        List<Parada> ruta = new ArrayList<>();
        paradas.forEachRemaining(ruta::add);
        assertEquals(1, ruta.size());
        assertEquals(prestamo.getId(), ruta.get(0).getPrestamoId());

        var asincrono = CobranzaServiceGrpc.newStub(autenticado);

        // Streaming de cliente: dos abonos válidos y uno inválido en un mismo lote
        CountDownLatch finLote = new CountDownLatch(1);
        AtomicReference<ResumenAbonos> resumen = new AtomicReference<>();
        StreamObserver<AbonoRequest> lote = asincrono.registrarAbonos(observador(resumen::set, finLote));
        lote.onNext(abono("a1", prestamo.getId(), 30));
        lote.onNext(abono("a2", prestamo.getId(), -5));
        lote.onNext(abono("a3", prestamo.getId(), 30));
        lote.onCompleted();
        assertTrue(finLote.await(20, TimeUnit.SECONDS));
        assertEquals(3, resumen.get().getRecibidos());
        assertEquals(2, resumen.get().getAceptados());
        assertEquals(1, resumen.get().getRechazados());
        assertEquals(60.0, resumen.get().getTotalRegistrado());

        // Bidireccional: cada abono recibe su confirmación
        List<ConfirmacionAbono> confirmaciones = new ArrayList<>();
        CountDownLatch finSync = new CountDownLatch(1);
        StreamObserver<AbonoRequest> sync = asincrono.sincronizarCobros(observador(confirmaciones::add, finSync));
        sync.onNext(abono("b1", prestamo.getId(), 30));
        sync.onNext(abono("b2", 999999L, 30));
        sync.onCompleted();
        assertTrue(finSync.await(20, TimeUnit.SECONDS));
        assertEquals(2, confirmaciones.size());
        assertTrue(confirmaciones.get(0).getAceptado());
        assertEquals("b1", confirmaciones.get(0).getIdLocal());
        assertEquals(30.0, confirmaciones.get(0).getSaldoPrestamo(), 0.01);
        assertFalse(confirmaciones.get(1).getAceptado());
        assertFalse(confirmaciones.get(1).getError().isBlank());

        // Las métricas y la información del servidor reflejan las llamadas
        var cobranza = info.info().servicios().stream()
                .filter(s -> s.nombre().equals("crediruta.v1.CobranzaService")).findFirst().orElseThrow();
        assertEquals(3, cobranza.metodos().size());
        assertTrue(cobranza.metodos().stream().anyMatch(m -> m.patron().equals("BIDI_STREAMING") && m.llamadas() >= 1));
        assertEquals(3, info.contratos().size());
    }

    private static <T> StreamObserver<T> observador(java.util.function.Consumer<T> alRecibir, CountDownLatch fin) {
        return new StreamObserver<>() {
            @Override public void onNext(T valor) { synchronized (this) { alRecibir.accept(valor); } }
            @Override public void onError(Throwable t) { fin.countDown(); }
            @Override public void onCompleted() { fin.countDown(); }
        };
    }

    private static AbonoRequest abono(String id, long prestamoId, double monto) {
        return AbonoRequest.newBuilder().setIdLocal(id).setPrestamoId(prestamoId).setMonto(monto).build();
    }

    private Cobrador nuevoCobrador() {
        long n = System.nanoTime();
        CobradorDTO dto = new CobradorDTO();
        dto.setNombre("Cobrador gRPC " + n);
        dto.setDocumento("GRPC-" + n);
        dto.setZona("Zona gRPC");
        dto.setCajaBase(100.0);
        return cobradorService.crear(dto);
    }

    private Prestamo nuevoPrestamo(Cobrador cobrador) {
        long n = System.nanoTime();
        Cliente c = new Cliente();
        c.setCedula("GRPC-C-" + n);
        c.setNombre("Cliente gRPC " + n);
        c.setDireccion("Calle gRPC");
        c.setTelefono("0990000000");
        c.setOrdenRuta(1);
        Cliente cliente = clienteService.crearCliente(c, cobrador.getId());
        PrestamoRequestDTO solicitud = new PrestamoRequestDTO(cliente.getId(), 100.0, TipoLiquidacion.DIARIO);
        solicitud.setNumeroCuotas(4);
        return prestamoService.crearPrestamo(solicitud, LocalDate.now().minusDays(2));
    }
}
