package com.example.cobro.model;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.List;

/**
 * Agente cobrador que recorre una ruta en terreno registrando abonos diarios.
 * Cada cobrador tiene una caja base con la que sale a la calle y cuadra al final del día.
 */
@Entity
@Table(name = "cobradores")
public class Cobrador extends Usuario {

    @Column(nullable = false, unique = true)
    private String documento;

    private String telefono;

    /** Nombre de la zona o ruta asignada (ej. "Ruta Norte"). */
    @Column(nullable = false)
    private String zona;

    /** Efectivo base con el que el cobrador inicia la jornada. */
    @Column(nullable = false)
    private Double cajaBase = 0.0;

    public Cobrador() {}

    public Cobrador(String nombre, String documento, String zona) {
        setNombre(nombre);
        this.documento = documento;
        this.zona = zona;
    }

    // Implementacion polimorfica de autenticar
    @Override
    public boolean autenticar() {
        return isActivo() && documento != null;
    }

    // Implementacion polimorfica de getPermisos
    @Override
    public List<String> getPermisos() {
        return Arrays.asList(
            "VER_RUTA_COBRO",
            "REGISTRAR_ABONOS",
            "CUADRAR_CAJA",
            "CONSULTAR_SCORING"
        );
    }

    @Override
    public String toString() {
        return "Cobrador{id=" + getId() +
               ", nombre=" + getNombre() +
               ", documento=" + documento +
               ", zona=" + zona + "}";
    }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public Double getCajaBase() { return cajaBase; }
    public void setCajaBase(Double cajaBase) { this.cajaBase = cajaBase; }
}
