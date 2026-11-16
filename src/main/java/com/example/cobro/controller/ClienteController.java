package com.example.cobro.controller;

import com.example.cobro.dto.ClienteRequestDTO;
import com.example.cobro.dto.ClienteResponseDTO;
import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.model.Cliente;
import com.example.cobro.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crear(@Valid @RequestBody ClienteRequestDTO dto) {
        Cliente creado = clienteService.crearCliente(dto.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponseDTO.fromEntity(creado));
    }

    @GetMapping
    public List<ClienteResponseDTO> listar() {
        return clienteService.listarClientes().stream().map(ClienteResponseDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO obtener(@PathVariable Long id) {
        return ClienteResponseDTO.fromEntity(clienteService.obtenerClienteOFallar(id));
    }

    @GetMapping("/cedula/{cedula}")
    public ClienteResponseDTO obtenerPorCedula(@PathVariable String cedula) {
        return clienteService.obtenerClientePorCedula(cedula)
                .map(ClienteResponseDTO::fromEntity)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con cédula: " + cedula));
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequestDTO dto) {
        return ClienteResponseDTO.fromEntity(clienteService.actualizarCliente(id, dto.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
