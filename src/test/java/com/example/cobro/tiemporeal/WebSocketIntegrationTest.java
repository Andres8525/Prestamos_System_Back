package com.example.cobro.tiemporeal;

import com.example.cobro.dto.AuthDTO;
import com.example.cobro.dto.CobradorDTO;
import com.example.cobro.dto.PrestamoRequestDTO;
import com.example.cobro.model.Cliente;
import com.example.cobro.model.Cobrador;
import com.example.cobro.model.Prestamo;
import com.example.cobro.model.TipoLiquidacion;
import com.example.cobro.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort private int puerto;
    @Autowired private AuthService authService;
    @Autowired private CobradorService cobradorService;
    @Autowired private ClienteService clienteService;
    @Autowired private PrestamoService prestamoService;
    @Autowired private AbonoService abonoService;
    @Autowired private RegistroConexiones conexiones;
    @Autowired private ObjectMapper json;

    private final BlockingQueue<JsonNode> recibidos = new LinkedBlockingQueue<>();

    private WebSocketSession conectar(String token) throws Exception {
        String url = "ws://localhost:" + puerto + "/ws/eventos" + (token != null ? "?token=" + token : "");
        return new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage m) throws Exception {
                recibidos.add(json.readTree(m.getPayload()));
            }
        }, url).get(10, TimeUnit.SECONDS);
    }

    private JsonNode esperar(String tipo) throws InterruptedException {
        long limite = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < limite) {
            JsonNode e = recibidos.poll(500, TimeUnit.MILLISECONDS);
            if (e != null && tipo.equals(e.path("tipo").asText())) return e;
        }
        fail("No llegó el evento " + tipo);
        return null;
    }

    @Test
    void sinTokenSeRechazaLaConexion() {
        assertThrows(ExecutionException.class, () -> conectar(null));
        assertThrows(ExecutionException.class, () -> conectar("token.invalido"));
    }

    @Test
    void elClienteRecibeBienvenidaPongYLosAbonosEnTiempoReal() throws Exception {
        String token = authService.registrar(new AuthDTO.Registro("WS", "ws" + System.nanoTime() + "@test.com",
                "Segura123")).token();
        WebSocketSession sesion = conectar(token);
        try {
            JsonNode bienvenida = esperar("BIENVENIDA");
            assertTrue(bienvenida.path("id").asLong() > 0);
            assertTrue(conexiones.cantidad() >= 1);

            sesion.sendMessage(new TextMessage("{\"accion\":\"ping\"}"));
            assertEquals("pong", esperar("LATIDO").path("mensaje").asText());

            Prestamo prestamo = nuevoPrestamo();
            esperar("PRESTAMO_CREADO");
            abonoService.registrarAbono(prestamo.getId(), 30.0, null, null, LocalDate.now());
            JsonNode abono = esperar("ABONO_REGISTRADO");
            assertEquals(prestamo.getId().longValue(), abono.path("datos").path("prestamoId").asLong());
            assertEquals(30.0, abono.path("datos").path("monto").asDouble());

            sesion.sendMessage(new TextMessage("{\"accion\":\"desuscribir\",\"tipos\":[\"ABONO_REGISTRADO\"]}"));
            Thread.sleep(300);
            recibidos.clear();
            abonoService.registrarAbono(prestamo.getId(), 30.0, null, null, LocalDate.now());
            Thread.sleep(800);
            assertTrue(recibidos.stream().noneMatch(e -> "ABONO_REGISTRADO".equals(e.path("tipo").asText())),
                    "tras desuscribirse no debe llegar el abono");
        } finally {
            sesion.close();
        }
    }

    private Prestamo nuevoPrestamo() {
        long n = System.nanoTime();
        CobradorDTO dto = new CobradorDTO();
        dto.setNombre("Cobrador WS " + n);
        dto.setDocumento("WS-" + n);
        dto.setZona("Zona WS");
        dto.setCajaBase(100.0);
        Cobrador cobrador = cobradorService.crear(dto);
        Cliente c = new Cliente();
        c.setCedula("WS-C-" + n);
        c.setNombre("Cliente WS " + n);
        c.setDireccion("Calle WS");
        c.setTelefono("0990000000");
        c.setOrdenRuta(1);
        Cliente cliente = clienteService.crearCliente(c, cobrador.getId());
        PrestamoRequestDTO solicitud = new PrestamoRequestDTO(cliente.getId(), 100.0, TipoLiquidacion.DIARIO);
        solicitud.setNumeroCuotas(4);
        return prestamoService.crearPrestamo(solicitud, LocalDate.now().minusDays(2));
    }
}
