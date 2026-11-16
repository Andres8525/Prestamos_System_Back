package com.example.cobro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de la tienda: productos que compra un cliente.
 * Si el pago es a CREDITO, el total se financia con un {@link Prestamo} que el cobrador recauda en su ruta.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    public enum TipoPago { CONTADO, CREDITO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPago tipoPago;

    @Column(nullable = false)
    private Double total = 0.0;

    /** Préstamo que financia el pedido (solo en ventas a crédito). */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prestamo_id")
    private Prestamo prestamo;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {}

    /** Agrega una línea al pedido y actualiza el total. */
    public void agregarDetalle(Producto producto, int cantidad) {
        DetallePedido detalle = new DetallePedido(this, producto, cantidad);
        detalles.add(detalle);
        total = Math.round((total + detalle.getSubtotal()) * 100.0) / 100.0;
    }

    public int totalUnidades() {
        return detalles.stream().mapToInt(DetallePedido::getCantidad).sum();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public TipoPago getTipoPago() { return tipoPago; }
    public void setTipoPago(TipoPago tipoPago) { this.tipoPago = tipoPago; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }
}
