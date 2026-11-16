package com.example.cobro.dto;

import com.example.cobro.model.Cobrador;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de entrada/salida para los agentes cobradores. */
public class CobradorDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El documento es obligatorio")
    private String documento;

    @Email(message = "El email no es válido")
    private String email;

    private String telefono;

    @NotBlank(message = "La zona/ruta es obligatoria")
    private String zona;

    @DecimalMin(value = "0.0", message = "La caja base no puede ser negativa")
    private Double cajaBase;

    private Boolean activo;

    /** Solo lectura: número de clientes asignados a su ruta. */
    private Long clientesAsignados;

    public CobradorDTO() {}

    public static CobradorDTO fromEntity(Cobrador c) {
        CobradorDTO dto = new CobradorDTO();
        dto.id = c.getId();
        dto.nombre = c.getNombre();
        dto.documento = c.getDocumento();
        dto.email = c.getEmail();
        dto.telefono = c.getTelefono();
        dto.zona = c.getZona();
        dto.cajaBase = c.getCajaBase();
        dto.activo = c.isActivo();
        return dto;
    }

    public void aplicarA(Cobrador c) {
        c.setNombre(nombre.trim());
        c.setDocumento(documento.trim());
        c.setEmail(email);
        c.setTelefono(telefono);
        c.setZona(zona.trim());
        c.setCajaBase(cajaBase != null ? cajaBase : 0.0);
        if (activo != null) c.setActivo(activo);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public Double getCajaBase() { return cajaBase; }
    public void setCajaBase(Double cajaBase) { this.cajaBase = cajaBase; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Long getClientesAsignados() { return clientesAsignados; }
    public void setClientesAsignados(Long clientesAsignados) { this.clientesAsignados = clientesAsignados; }
}
