package com.example.cobro.controller;

import com.example.cobro.dto.PagoResponseDTO;
import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.model.Pago;
import com.example.cobro.service.PagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @GetMapping
    public List<PagoResponseDTO> listar() {
        return map(pagoService.listarPagos());
    }

    @GetMapping("/{id}")
    public PagoResponseDTO obtener(@PathVariable Long id) {
        return pagoService.obtenerPago(id).map(PagoResponseDTO::fromEntity)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", id));
    }

    @GetMapping("/prestamo/{prestamoId}")
    public List<PagoResponseDTO> listarPorPrestamo(@PathVariable Long prestamoId) {
        return map(pagoService.listarPorPrestamo(prestamoId));
    }

    @GetMapping("/prestamo/{prestamoId}/pendientes")
    public List<PagoResponseDTO> listarPendientes(@PathVariable Long prestamoId) {
        return map(pagoService.listarPendientesPorPrestamo(prestamoId));
    }

    @GetMapping("/prestamo/{prestamoId}/realizados")
    public List<PagoResponseDTO> listarRealizados(@PathVariable Long prestamoId) {
        return map(pagoService.listarRealizadosPorPrestamo(prestamoId));
    }

    @GetMapping("/atrasados")
    public List<PagoResponseDTO> listarAtrasados() {
        return map(pagoService.listarAtrasados());
    }

    @GetMapping("/contar-pendientes")
    public long contarPendientes() {
        return pagoService.contarPendientes();
    }

    @PostMapping("/{id}/pagar")
    public PagoResponseDTO marcarPagado(@PathVariable Long id) {
        return PagoResponseDTO.fromEntity(pagoService.marcarPagoRealizado(id));
    }

    @PutMapping("/{id}")
    public PagoResponseDTO actualizar(@PathVariable Long id, @RequestBody Pago pago) {
        return PagoResponseDTO.fromEntity(pagoService.actualizarPago(id, pago));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pagoService.eliminarPago(id);
        return ResponseEntity.noContent().build();
    }

    private List<PagoResponseDTO> map(List<Pago> pagos) {
        return pagos.stream().map(PagoResponseDTO::fromEntity).toList();
    }
}
