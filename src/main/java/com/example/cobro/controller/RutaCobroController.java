package com.example.cobro.controller;

import com.example.cobro.dto.RutaCobroDTO;
import com.example.cobro.estructuras.PrioridadCobro;
import com.example.cobro.service.RutaCobroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/rutas")
public class RutaCobroController {

    @Autowired
    private RutaCobroService rutaCobroService;

    /** Hoja de ruta del cobrador para una fecha (por defecto hoy). */
    @GetMapping("/cobrador/{cobradorId}")
    public RutaCobroDTO rutaDelDia(
            @PathVariable Long cobradorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return rutaCobroService.generarRuta(cobradorId, fecha != null ? fecha : LocalDate.now());
    }

    /** Misma ruta ordenada por urgencia: cobro exigible hoy, días de atraso y monto. */
    @GetMapping("/cobrador/{cobradorId}/priorizada")
    public RutaCobroDTO rutaPriorizada(
            @PathVariable Long cobradorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        RutaCobroDTO ruta = rutaCobroService.generarRuta(cobradorId, fecha != null ? fecha : LocalDate.now());
        return new RutaCobroDTO(ruta.cobradorId(), ruta.cobradorNombre(), ruta.zona(), ruta.fecha(),
                ruta.totalVisitas(), ruta.visitasConAbono(), ruta.totalExigible(), ruta.totalRecaudado(),
                PrioridadCobro.ordenar(ruta.paradas()));
    }
}
