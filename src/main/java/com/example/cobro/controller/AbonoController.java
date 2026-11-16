package com.example.cobro.controller;

import com.example.cobro.dto.AbonoRequestDTO;
import com.example.cobro.dto.AbonoResponseDTO;
import com.example.cobro.model.Abono;
import com.example.cobro.service.AbonoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/abonos")
public class AbonoController {

    @Autowired
    private AbonoService abonoService;

    /** Registra un abono (pago diario) recibido en la calle. */
    @PostMapping
    public ResponseEntity<AbonoResponseDTO> registrar(@Valid @RequestBody AbonoRequestDTO dto) {
        AbonoResponseDTO abono = abonoService.registrarAbono(
                dto.getPrestamoId(), dto.getMonto(), dto.getCobradorId(), dto.getObservacion());
        return ResponseEntity.status(HttpStatus.CREATED).body(abono);
    }

    @GetMapping("/prestamo/{prestamoId}")
    public List<AbonoResponseDTO> porPrestamo(@PathVariable Long prestamoId) {
        return map(abonoService.listarPorPrestamo(prestamoId));
    }

    @GetMapping("/cobrador/{cobradorId}")
    public List<AbonoResponseDTO> porCobrador(
            @PathVariable Long cobradorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return map(abonoService.listarPorCobradorYFecha(cobradorId, fecha != null ? fecha : LocalDate.now()));
    }

    @GetMapping
    public List<AbonoResponseDTO> porFecha(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return map(abonoService.listarPorFecha(fecha != null ? fecha : LocalDate.now()));
    }

    private List<AbonoResponseDTO> map(List<Abono> abonos) {
        return abonos.stream().map(AbonoResponseDTO::fromEntity).toList();
    }
}
