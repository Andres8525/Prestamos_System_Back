package com.example.cobro.controller;

import com.example.cobro.dto.CobradorDTO;
import com.example.cobro.model.Cobrador;
import com.example.cobro.repository.ClienteRepository;
import com.example.cobro.service.CobradorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cobradores")
public class CobradorController {

    @Autowired
    private CobradorService cobradorService;

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping
    public List<CobradorDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        var lista = soloActivos ? cobradorService.listarActivos() : cobradorService.listar();
        return lista.stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public CobradorDTO obtener(@PathVariable Long id) {
        return toDto(cobradorService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<CobradorDTO> crear(@Valid @RequestBody CobradorDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CobradorDTO.fromEntity(cobradorService.crear(dto)));
    }

    @PutMapping("/{id}")
    public CobradorDTO actualizar(@PathVariable Long id, @Valid @RequestBody CobradorDTO dto) {
        return CobradorDTO.fromEntity(cobradorService.actualizar(id, dto));
    }

    private CobradorDTO toDto(Cobrador cobrador) {
        CobradorDTO dto = CobradorDTO.fromEntity(cobrador);
        dto.setClientesAsignados(clienteRepository.countByCobrador_Id(cobrador.getId()));
        return dto;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        cobradorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
