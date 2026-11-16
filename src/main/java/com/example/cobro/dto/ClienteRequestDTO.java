package com.example.cobro.dto;

import com.example.cobro.model.Cliente;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ClienteRequestDTO {

    @NotBlank(message = "La cédula es obligatoria")
    private String cedula;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "[0-9\\s\\-\\+\\(\\)]{7,15}", message = "El teléfono debe tener entre 7 y 15 caracteres")
    private String telefono;

    /** Cobrador al que se asigna el cliente (opcional). */
    private Long cobradorId;

    @Min(value = 1, message = "El orden de ruta debe ser mayor o igual a 1")
    private Integer ordenRuta;

    public ClienteRequestDTO() {}

    public ClienteRequestDTO(String cedula, String nombre, String direccion, String telefono) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
    }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Long getCobradorId() { return cobradorId; }
    public void setCobradorId(Long cobradorId) { this.cobradorId = cobradorId; }

    public Integer getOrdenRuta() { return ordenRuta; }
    public void setOrdenRuta(Integer ordenRuta) { this.ordenRuta = ordenRuta; }

    public Cliente toEntity() {
        Cliente cliente = new Cliente();
        cliente.setCedula(cedula != null ? cedula.trim() : null);
        cliente.setNombre(nombre != null ? nombre.trim() : null);
        cliente.setDireccion(direccion != null ? direccion.trim() : null);
        cliente.setTelefono(telefono != null ? telefono.trim() : null);
        cliente.setOrdenRuta(ordenRuta);
        return cliente;
    }
}
