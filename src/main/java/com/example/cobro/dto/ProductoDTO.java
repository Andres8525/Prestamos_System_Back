package com.example.cobro.dto;

import com.example.cobro.model.Producto;
import jakarta.validation.constraints.*;

public record ProductoDTO(
        Long id,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @NotBlank(message = "La categoría es obligatoria")
        String categoria,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        Double precio,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @Size(max = 500, message = "La URL de la imagen no puede superar 500 caracteres")
        @Pattern(regexp = "^$|^https?://.+", message = "La imagen debe ser una URL http(s)")
        String imagenUrl,

        Boolean activo
) {
    public static ProductoDTO fromEntity(Producto p) {
        return new ProductoDTO(p.getId(), p.getNombre(), p.getDescripcion(), p.getCategoria(),
                p.getPrecio(), p.getStock(), p.getImagenUrl(), p.isActivo());
    }

    public void aplicarA(Producto p) {
        p.setNombre(nombre.trim());
        p.setDescripcion(descripcion);
        p.setCategoria(categoria.trim());
        p.setPrecio(Math.round(precio * 100.0) / 100.0);
        p.setStock(stock);
        p.setImagenUrl(imagenUrl == null || imagenUrl.isBlank() ? null : imagenUrl.trim());
        if (activo != null) p.setActivo(activo);
    }
}
