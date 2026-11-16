package com.example.cobro.tiemporeal;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Conexiones abiertas indexadas por id de sesión (búsqueda y baja en O(1), seguro entre hilos). */
@Component
public class RegistroConexiones {

    private final Map<String, ConexionCliente> conexiones = new ConcurrentHashMap<>();

    public void agregar(ConexionCliente conexion) {
        conexiones.put(conexion.sesion().getId(), conexion);
    }

    public Optional<ConexionCliente> obtener(String sesionId) {
        return Optional.ofNullable(conexiones.get(sesionId));
    }

    public void quitar(String sesionId) {
        conexiones.remove(sesionId);
    }

    public Collection<ConexionCliente> todas() {
        return conexiones.values();
    }

    public int cantidad() {
        return conexiones.size();
    }

    public long usuariosDistintos() {
        return conexiones.values().stream().map(ConexionCliente::usuarioId).distinct().count();
    }
}
