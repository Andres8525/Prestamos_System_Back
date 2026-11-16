package com.example.cobro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Cuenta de acceso al sistema (prestamista / administrador de la cartera).
 * La contraseña heredada de {@link Usuario} se guarda siempre como hash, nunca en texto plano.
 */
@Entity
@Table(name = "administradores",
       uniqueConstraints = @UniqueConstraint(name = "uk_admin_email", columnNames = "email"))
public class Administrador extends Usuario {

    private String codigoAdmin;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    private LocalDateTime ultimoAcceso;

    public Administrador() {}

    public Administrador(Long id, String nombre, String email, String contrasena, String codigoAdmin) {
        super(id, nombre, email, contrasena);
        this.codigoAdmin = codigoAdmin;
    }

    // Implementacion polimorfica de autenticar
    @Override
    public boolean autenticar() {
        return isActivo() && getEmail() != null && getContrasena() != null;
    }

    // Implementacion polimorfica de getPermisos
    @Override
    public List<String> getPermisos() {
        return Arrays.asList(
            "GESTIONAR_CLIENTES",
            "GESTIONAR_COBRADORES",
            "CREAR_PRESTAMOS",
            "REGISTRAR_PAGOS",
            "CUADRAR_CAJA",
            "VER_SCORING_IA",
            "VER_PANEL",
            "GENERAR_REPORTES"
        );
    }

    // Implementacion polimorfica de toString
    @Override
    public String toString() {
        return "Administrador{id=" + getId() +
               ", nombre=" + getNombre() +
               ", email=" + getEmail() +
               ", codigoAdmin=" + codigoAdmin + "}";
    }

    public String getCodigoAdmin() { return codigoAdmin; }
    public void setCodigoAdmin(String codigoAdmin) { this.codigoAdmin = codigoAdmin; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
}
