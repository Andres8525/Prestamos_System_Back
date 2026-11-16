package com.example.cobro.controller;

import com.example.cobro.dto.PrestamoRequestDTO;
import com.example.cobro.dto.PrestamoResponseDTO;
import com.example.cobro.dto.SimulacionPrestamoDTO;
import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Prestamo;
import com.example.cobro.service.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    @Autowired
    private PrestamoService prestamoService;

    @PostMapping
    public ResponseEntity<PrestamoResponseDTO> crear(@Valid @RequestBody PrestamoRequestDTO dto) {
        Prestamo prestamo = prestamoService.crearPrestamo(dto, LocalDate.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(PrestamoResponseDTO.fromEntity(prestamo));
    }

    /** Calcula intereses, cuota y vencimiento sin emitir el préstamo. */
    @PostMapping("/simular")
    public SimulacionPrestamoDTO simular(@Valid @RequestBody PrestamoRequestDTO dto) {
        return prestamoService.simular(dto, LocalDate.now());
    }

    @GetMapping
    public List<PrestamoResponseDTO> listar() {
        return map(prestamoService.listarPrestamos());
    }

    @GetMapping("/{id}")
    public PrestamoResponseDTO obtener(@PathVariable Long id) {
        return PrestamoResponseDTO.fromEntity(prestamoService.obtenerPrestamoOFallar(id));
    }

    @GetMapping("/cliente/{clienteId}")
    public List<PrestamoResponseDTO> listarPorCliente(@PathVariable Long clienteId) {
        return map(prestamoService.listarPorCliente(clienteId));
    }

    @GetMapping("/cobrador/{cobradorId}")
    public List<PrestamoResponseDTO> listarPorCobrador(@PathVariable Long cobradorId) {
        return map(prestamoService.listarPorCobrador(cobradorId));
    }

    @GetMapping("/estado/al-dia")
    public List<PrestamoResponseDTO> clientesAlDia() {
        return map(prestamoService.listarPorEstado(EstadoPago.AL_DIA));
    }

    @GetMapping("/estado/pendientes")
    public List<PrestamoResponseDTO> clientesPendientes() {
        return map(prestamoService.listarPorEstado(EstadoPago.PENDIENTE));
    }

    @GetMapping("/estado/atrasados")
    public List<PrestamoResponseDTO> clientesAtrasados() {
        return map(prestamoService.listarPorEstado(EstadoPago.ATRASADO));
    }

    @GetMapping("/total-mes")
    public Double totalMes(@RequestParam int year, @RequestParam int month) {
        return prestamoService.obtenerTotalMes(year, month);
    }

    @PutMapping("/{id}")
    public PrestamoResponseDTO actualizar(@PathVariable Long id, @RequestBody Prestamo prestamo) {
        return PrestamoResponseDTO.fromEntity(prestamoService.actualizarPrestamo(id, prestamo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        prestamoService.eliminarPrestamo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/actualizar-estados")
    public Map<String, String> actualizarEstados() {
        prestamoService.actualizarEstados();
        return Map.of("mensaje", "Estados actualizados correctamente");
    }

    private List<PrestamoResponseDTO> map(List<Prestamo> prestamos) {
        return prestamos.stream().map(PrestamoResponseDTO::fromEntity).toList();
    }
}
