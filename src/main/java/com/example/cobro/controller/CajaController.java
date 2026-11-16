package com.example.cobro.controller;

import com.example.cobro.dto.CierreCajaRequestDTO;
import com.example.cobro.dto.CuadreCajaDTO;
import com.example.cobro.service.CajaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    @Autowired
    private CajaService cajaService;

    /** Balance del día del cobrador (vista previa si la caja sigue abierta). */
    @GetMapping("/cobrador/{cobradorId}")
    public CuadreCajaDTO consultar(
            @PathVariable Long cobradorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Double baseInicial,
            @RequestParam(required = false) Double gastos) {
        return cajaService.consultar(cobradorId, fecha != null ? fecha : LocalDate.now(), baseInicial, gastos);
    }

    @GetMapping("/cobrador/{cobradorId}/historial")
    public List<CuadreCajaDTO> historial(@PathVariable Long cobradorId) {
        return cajaService.historial(cobradorId);
    }

    /** Registra el cierre (cuadre) de caja del día. */
    @PostMapping("/cierre")
    public ResponseEntity<CuadreCajaDTO> cerrar(@Valid @RequestBody CierreCajaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.cerrar(dto));
    }
}
