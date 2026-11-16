package com.example.cobro.dto;

import com.example.cobro.model.Cliente;

public class ClienteResponseDTO {

    private Long id;
    private String cedula;
    private String nombre;
    private String direccion;
    private String telefono;
    private Long cobradorId;
    private String cobradorNombre;
    private String zona;
    private Integer ordenRuta;

    public ClienteResponseDTO() {}

    public static ClienteResponseDTO fromEntity(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setId(cliente.getId());
        dto.setCedula(cliente.getCedula());
        dto.setNombre(cliente.getNombre());
        dto.setDireccion(cliente.getDireccion());
        dto.setTelefono(cliente.getTelefono());
        if (cliente.getCobrador() != null) {
            dto.setCobradorId(cliente.getCobrador().getId());
            dto.setCobradorNombre(cliente.getCobrador().getNombre());
            dto.setZona(cliente.getCobrador().getZona());
        }
        dto.setOrdenRuta(cliente.getOrdenRuta());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getCobradorNombre() { return cobradorNombre; }
    public void setCobradorNombre(String cobradorNombre) { this.cobradorNombre = cobradorNombre; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public Integer getOrdenRuta() { return ordenRuta; }
    public void setOrdenRuta(Integer ordenRuta) { this.ordenRuta = ordenRuta; }
}
