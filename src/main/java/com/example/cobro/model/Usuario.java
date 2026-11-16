package com.example.cobro.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.List;

/**
 * Clase base abstracta de los usuarios del sistema (Administrador, Cobrador).
 * Es @MappedSuperclass: sus atributos se heredan en las tablas de las subclases que sean entidades.
 */
@MappedSuperclass
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasena;

    @Column(nullable = false)
    private boolean activo = true;

    public Usuario() {}

    public Usuario(Long id, String nombre, String email, String contrasena) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.contrasena = contrasena;
        this.activo = true;
    }

    // Metodos abstractos — polimorfismo
    public abstract boolean autenticar();
    public abstract List<String> getPermisos();

    @Override
    public abstract String toString();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
