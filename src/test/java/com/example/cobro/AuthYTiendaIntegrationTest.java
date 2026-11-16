package com.example.cobro;

import com.example.cobro.dto.CobradorDTO;
import com.example.cobro.dto.PedidoDTO;
import com.example.cobro.dto.ProductoDTO;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.Cliente;
import com.example.cobro.model.Pedido;
import com.example.cobro.model.Producto;
import com.example.cobro.model.TipoLiquidacion;
import com.example.cobro.repository.AdministradorRepository;
import com.example.cobro.security.PasswordHasher;
import com.example.cobro.service.ClienteService;
import com.example.cobro.service.CobradorService;
import com.example.cobro.service.TiendaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.example.cobro.security.AuthFilter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthYTiendaIntegrationTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private AuthFilter authFilter;
    private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private AdministradorRepository administradorRepository;
    @Autowired private PasswordHasher passwordHasher;
    @Autowired private TiendaService tiendaService;
    @Autowired private ClienteService clienteService;
    @Autowired private CobradorService cobradorService;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(authFilter).build();
    }

    @Test
    void laApiExigeTokenYElRegistroPermiteIniciarSesion() throws Exception {
        mvc.perform(get("/api/cobradores")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/cobradores").header("Authorization", "Bearer token.falso.xyz"))
                .andExpect(status().isUnauthorized());

        String registro = json.writeValueAsString(Map.of(
                "nombre", "Ana Pruebas", "email", "Ana@Test.com", "password", "Segura123"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.email").value("ana@test.com"));
        // El email no puede repetirse y la contraseña se guarda como hash.
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isBadRequest());
        String guardada = administradorRepository.findByEmail("ana@test.com").orElseThrow().getContrasena();
        assertNotEquals("Segura123", guardada);
        assertTrue(passwordHasher.coincide("Segura123", guardada));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "ana@test.com", "password", "incorrecta1"))))
                .andExpect(status().isUnauthorized());

        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "ana@test.com", "password", "Segura123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(respuesta).get("token").asText();

        mvc.perform(get("/api/cobradores").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Pruebas"));
        // Un token manipulado se rechaza.
        mvc.perform(get("/api/cobradores").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroRechazaContrasenasDebiles() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nombre", "X", "email", "debil@test.com", "password", "corta"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void ventaACreditoDescuentaStockYGeneraElPrestamo() {
        CobradorDTO cobradorDto = new CobradorDTO();
        cobradorDto.setNombre("Cobrador Tienda");
        cobradorDto.setDocumento("TIENDA-1");
        cobradorDto.setZona("Zona Tienda");
        Long cobradorId = cobradorService.crear(cobradorDto).getId();

        Cliente cliente = new Cliente();
        cliente.setCedula("TIENDA-CLI-1");
        cliente.setNombre("Cliente Tienda");
        cliente.setDireccion("Calle 1");
        cliente.setTelefono("0990000001");
        cliente = clienteService.crearCliente(cliente, cobradorId);

        Producto licuadora = tiendaService.crearProducto(
                new ProductoDTO(null, "Licuadora", null, "Hogar", 40.0, 5, null, true));
        Producto olla = tiendaService.crearProducto(
                new ProductoDTO(null, "Olla", null, "Hogar", 10.0, 2, null, true));

        Pedido pedido = tiendaService.crearPedido(new PedidoDTO.Solicitud(
                cliente.getId(), Pedido.TipoPago.CREDITO,
                List.of(new PedidoDTO.Item(licuadora.getId(), 2), new PedidoDTO.Item(olla.getId(), 2)),
                TipoLiquidacion.DIARIO, 10, 20.0, null, null));

        assertEquals(100.0, pedido.getTotal());
        assertEquals(4, pedido.totalUnidades());
        assertNotNull(pedido.getPrestamo(), "La venta a crédito genera un préstamo");
        assertEquals(120.0, pedido.getPrestamo().getMontoTotal());
        assertEquals(12.0, pedido.getPrestamo().getValorCuota());
        assertEquals(cobradorId, pedido.getPrestamo().getCobrador().getId());
        assertEquals(3, tiendaService.obtenerProducto(licuadora.getId()).getStock());
        assertEquals(0, tiendaService.obtenerProducto(olla.getId()).getStock());

        Long clienteId = cliente.getId();
        ReglaNegocioException sinStock = assertThrows(ReglaNegocioException.class, () ->
                tiendaService.crearPedido(new PedidoDTO.Solicitud(clienteId, Pedido.TipoPago.CONTADO,
                        List.of(new PedidoDTO.Item(licuadora.getId(), 1), new PedidoDTO.Item(olla.getId(), 1)),
                        null, null, null, null, null)));
        assertTrue(sinStock.getMessage().contains("Stock insuficiente"));
        assertEquals(3, tiendaService.obtenerProducto(licuadora.getId()).getStock(),
                "Un pedido rechazado no debe descontar inventario");

        Pedido contado = tiendaService.crearPedido(new PedidoDTO.Solicitud(clienteId, Pedido.TipoPago.CONTADO,
                List.of(new PedidoDTO.Item(licuadora.getId(), 1)), null, null, null, null, null));
        assertNull(contado.getPrestamo());
        assertEquals(2, tiendaService.listarPedidosPorCliente(clienteId).size());
    }
}
