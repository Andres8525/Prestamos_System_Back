package com.example.cobro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Parámetro interno del sistema guardado como clave/valor (por ejemplo, la clave de firma de sesiones). */
@Entity
@Table(name = "parametros_sistema")
public class ParametroSistema {

    @Id
    @Column(length = 80)
    private String clave;

    @Column(nullable = false, length = 500)
    private String valor;

    public ParametroSistema() {}

    public ParametroSistema(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() { return clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
