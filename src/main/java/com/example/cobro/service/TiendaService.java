package com.example.cobro.service;

import com.example.cobro.dto.PedidoDTO;
import com.example.cobro.dto.PrestamoRequestDTO;
import com.example.cobro.dto.ProductoDTO;
import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.*;
import com.example.cobro.repository.ClienteRepository;
import com.example.cobro.repository.PedidoRepository;
import com.example.cobro.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tienda (e-commerce): catálogo de productos y pedidos.
 * Una venta a crédito descuenta el inventario y emite un préstamo por el total del pedido,
 * pasando por el mismo control de riesgo (scoring IA) que cualquier otro crédito.
 */
@Service
@Transactional
public class TiendaService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PrestamoService prestamoService;

    // ------------------------------------------------------------ catálogo

    @Transactional(readOnly = true)
    public List<Producto> listarProductos(boolean soloActivos) {
        return soloActivos ? productoRepository.findByActivoTrueOrderByCategoriaAscNombreAsc()
                : productoRepository.findAllByOrderByCategoriaAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public Producto obtenerProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Producto", id));
    }

    public Producto crearProducto(ProductoDTO dto) {
        Producto producto = new Producto();
        dto.aplicarA(producto);
        return productoRepository.save(producto);
    }

    public Producto actualizarProducto(Long id, ProductoDTO dto) {
        Producto producto = obtenerProducto(id);
        dto.aplicarA(producto);
        return productoRepository.save(producto);
    }

    /** Baja lógica: el producto deja de ofrecerse pero se conserva en los pedidos anteriores. */
    public void desactivarProducto(Long id) {
        Producto producto = obtenerProducto(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    // ------------------------------------------------------------ pedidos

    public Pedido crearPedido(PedidoDTO.Solicitud solicitud) {
        Cliente cliente = clienteRepository.findById(solicitud.clienteId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", solicitud.clienteId()));

        // Si el mismo producto viene repetido en el carrito se suman las cantidades.
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        solicitud.items().forEach(i -> cantidades.merge(i.productoId(), i.cantidad(), Integer::sum));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setTipoPago(solicitud.tipoPago());

        for (Map.Entry<Long, Integer> item : cantidades.entrySet()) {
            Producto producto = obtenerProducto(item.getKey());
            int cantidad = item.getValue();
            if (!producto.isActivo()) {
                throw new ReglaNegocioException("El producto " + producto.getNombre() + " ya no está disponible");
            }
            if (!producto.hayStock(cantidad)) {
                throw new ReglaNegocioException("Stock insuficiente de " + producto.getNombre()
                        + " (disponible: " + producto.getStock() + ")");
            }
            producto.descontarStock(cantidad);
            productoRepository.save(producto);
            pedido.agregarDetalle(producto, cantidad);
        }

        if (solicitud.tipoPago() == Pedido.TipoPago.CREDITO) {
            pedido.setPrestamo(financiar(solicitud, pedido.getTotal()));
        }
        return pedidoRepository.save(pedido);
    }

    /**
     * Emite el préstamo que financia el pedido. Si el scoring bloquea la operación se lanza
     * RiesgoCreditoException y la transacción completa (incluido el descuento de stock) se revierte.
     */
    private Prestamo financiar(PedidoDTO.Solicitud solicitud, double total) {
        if (solicitud.tipoLiquidacion() == null) {
            throw new ReglaNegocioException("Para vender a crédito indique el tipo de liquidación");
        }
        PrestamoRequestDTO dto = new PrestamoRequestDTO(solicitud.clienteId(), total, solicitud.tipoLiquidacion());
        dto.setNumeroCuotas(solicitud.numeroCuotas());
        dto.setTasaInteres(solicitud.tasaInteres());
        dto.setCobradorId(solicitud.cobradorId());
        dto.setForzarAprobacion(solicitud.forzarAprobacion());
        return prestamoService.crearPrestamo(dto, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosPorCliente(Long clienteId) {
        return pedidoRepository.findByCliente_IdOrderByFechaDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public Pedido obtenerPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pedido", id));
    }
}
