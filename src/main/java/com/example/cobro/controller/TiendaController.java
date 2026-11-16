package com.example.cobro.controller;

import com.example.cobro.dto.PedidoDTO;
import com.example.cobro.dto.ProductoDTO;
import com.example.cobro.service.TiendaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** API de la tienda: catálogo de productos y pedidos (contado o crédito). */
@RestController
@RequestMapping("/api")
public class TiendaController {

    @Autowired
    private TiendaService tiendaService;

    // --- Productos
    @GetMapping("/productos")
    public List<ProductoDTO> listarProductos(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return tiendaService.listarProductos(soloActivos).stream().map(ProductoDTO::fromEntity).toList();
    }

    @GetMapping("/productos/{id}")
    public ProductoDTO obtenerProducto(@PathVariable Long id) {
        return ProductoDTO.fromEntity(tiendaService.obtenerProducto(id));
    }

    @PostMapping("/productos")
    public ResponseEntity<ProductoDTO> crearProducto(@Valid @RequestBody ProductoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductoDTO.fromEntity(tiendaService.crearProducto(dto)));
    }

    @PutMapping("/productos/{id}")
    public ProductoDTO actualizarProducto(@PathVariable Long id, @Valid @RequestBody ProductoDTO dto) {
        return ProductoDTO.fromEntity(tiendaService.actualizarProducto(id, dto));
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Long id) {
        tiendaService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    // --- Pedidos
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoDTO.Respuesta> crearPedido(@Valid @RequestBody PedidoDTO.Solicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PedidoDTO.Respuesta.fromEntity(tiendaService.crearPedido(solicitud)));
    }

    @GetMapping("/pedidos")
    public List<PedidoDTO.Respuesta> listarPedidos() {
        return tiendaService.listarPedidos().stream().map(PedidoDTO.Respuesta::fromEntity).toList();
    }

    @GetMapping("/pedidos/{id}")
    public PedidoDTO.Respuesta obtenerPedido(@PathVariable Long id) {
        return PedidoDTO.Respuesta.fromEntity(tiendaService.obtenerPedido(id));
    }

    @GetMapping("/pedidos/cliente/{clienteId}")
    public List<PedidoDTO.Respuesta> pedidosPorCliente(@PathVariable Long clienteId) {
        return tiendaService.listarPedidosPorCliente(clienteId).stream()
                .map(PedidoDTO.Respuesta::fromEntity).toList();
    }
}
