package com.example.cobro.model;

import jakarta.persistence.*;

/** Línea de un pedido: guarda el nombre y el precio del producto en el momento de la venta. */
@Entity
@Table(name = "detalles_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private String nombreProducto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Double precioUnitario;

    @Column(nullable = false)
    private Double subtotal;

    public DetallePedido() {}

    public DetallePedido(Pedido pedido, Producto producto, int cantidad) {
        this.pedido = pedido;
        this.producto = producto;
        this.nombreProducto = producto.getNombre();
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
        this.subtotal = Math.round(producto.getPrecio() * cantidad * 100.0) / 100.0;
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public Producto getProducto() { return producto; }
    public String getNombreProducto() { return nombreProducto; }
    public Integer getCantidad() { return cantidad; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public Double getSubtotal() { return subtotal; }
}
