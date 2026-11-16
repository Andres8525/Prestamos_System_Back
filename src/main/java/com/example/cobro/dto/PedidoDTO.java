package com.example.cobro.dto;

import com.example.cobro.model.Pedido;
import com.example.cobro.model.TipoLiquidacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de la tienda: solicitud de compra (carrito) y pedido confirmado. */
public final class PedidoDTO {

    private PedidoDTO() {}

    public record Item(
            @NotNull(message = "El producto es obligatorio") Long productoId,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad mínima es 1")
            @Max(value = 999, message = "La cantidad máxima es 999")
            Integer cantidad
    ) {}

    public record Solicitud(
            @NotNull(message = "El cliente es obligatorio") Long clienteId,
            @NotNull(message = "El tipo de pago es obligatorio") Pedido.TipoPago tipoPago,
            @NotEmpty(message = "El carrito está vacío") @Valid List<Item> items,
            // Solo para ventas a crédito:
            TipoLiquidacion tipoLiquidacion,
            @Min(value = 1, message = "Debe haber al menos una cuota")
            @Max(value = 365, message = "El número de cuotas no puede superar 365")
            Integer numeroCuotas,
            @DecimalMin(value = "0.0", message = "La tasa de interés no puede ser negativa")
            @DecimalMax(value = "100.0", message = "La tasa de interés no puede superar el 100%")
            Double tasaInteres,
            Long cobradorId,
            Boolean forzarAprobacion
    ) {}

    public record Linea(Long productoId, String nombreProducto, int cantidad,
                        double precioUnitario, double subtotal) {}

    public record Respuesta(
            Long id,
            Long clienteId,
            String clienteNombre,
            LocalDateTime fecha,
            Pedido.TipoPago tipoPago,
            double total,
            int unidades,
            Long prestamoId,
            Double totalFinanciado,
            Double valorCuota,
            Integer numeroCuotas,
            String nivelRiesgo,
            List<Linea> detalles
    ) {
        public static Respuesta fromEntity(Pedido p) {
            var prestamo = p.getPrestamo();
            return new Respuesta(
                    p.getId(), p.getCliente().getId(), p.getCliente().getNombre(), p.getFecha(),
                    p.getTipoPago(), p.getTotal(), p.totalUnidades(),
                    prestamo != null ? prestamo.getId() : null,
                    prestamo != null ? prestamo.getMontoTotal() : null,
                    prestamo != null ? prestamo.getValorCuota() : null,
                    prestamo != null ? prestamo.getNumeroCuotas() : null,
                    prestamo != null ? prestamo.getNivelRiesgo() : null,
                    p.getDetalles().stream().map(d -> new Linea(d.getProducto().getId(), d.getNombreProducto(),
                            d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal())).toList());
        }
    }
}
