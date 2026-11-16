package com.example.cobro.service;

import com.example.cobro.estructuras.Trie;
import com.example.cobro.model.Cliente;
import com.example.cobro.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Autocompletado de clientes por prefijo de nombre, apellido o cédula.
 * El índice (Trie) se construye la primera vez y se reconstruye cuando cambia la lista de clientes.
 */
@Service
@Transactional(readOnly = true)
public class BusquedaClientesService {

    public static final int LIMITE_MAXIMO = 20;

    private final ClienteRepository clienteRepository;
    private volatile Trie<Long> indice;
    private volatile Map<Long, Cliente> porId = Map.of();

    public BusquedaClientesService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> buscar(String texto, int limite) {
        Trie<Long> trie = indice;
        if (trie == null) trie = reconstruir();
        Map<Long, Cliente> clientes = porId;
        return trie.buscar(texto, Math.min(Math.max(1, limite), LIMITE_MAXIMO)).stream()
                .map(clientes::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Marca el índice como desactualizado; se reconstruye en la próxima búsqueda. */
    public void invalidar() {
        indice = null;
    }

    private synchronized Trie<Long> reconstruir() {
        if (indice != null) return indice;
        List<Cliente> todos = clienteRepository.findAll();
        Trie<Long> trie = new Trie<>();
        for (Cliente c : todos) {
            trie.insertar(c.getNombre(), c.getId());
            trie.insertar(c.getCedula(), c.getId());
        }
        porId = todos.stream().collect(Collectors.toMap(Cliente::getId, Function.identity()));
        indice = trie;
        return trie;
    }
}
