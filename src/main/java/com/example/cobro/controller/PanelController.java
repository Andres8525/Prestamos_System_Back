package com.example.cobro.controller;

import com.example.cobro.model.Panel;
import com.example.cobro.model.PanelItem;
import com.example.cobro.service.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panel")
public class PanelController {

    @Autowired
    private com.example.cobro.repository.PagoRepository pagoRepository;

    @Autowired
    private PanelService panelService;

    @GetMapping
    public ResponseEntity<Panel> obtenerPanel() {
        return ResponseEntity.ok(panelService.generarPanel());
    }

    /** Antigüedad de la cartera: cuotas pendientes y saldo agrupados por tramo de días de atraso. */
    @GetMapping("/antiguedad")
    public java.util.List<com.example.cobro.estructuras.TramosMora.Resumen> antiguedad() {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.util.List<long[]> pendientes = pagoRepository.findAll().stream()
                .filter(c -> c.getFechaPago() == null)
                .map(c -> new long[]{
                        Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(c.getFechaProgramada(), hoy)),
                        Math.round(c.saldo() * 100)})
                .toList();
        return new com.example.cobro.estructuras.TramosMora().resumir(pendientes);
    }

    @GetMapping("/clientes-adeuda")
    public ResponseEntity<PanelItem> clientesAdeuda() {
        Panel panel = panelService.generarPanel();
        PanelItem item = panel.getClientesAdeuda();
        return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
    }

    @GetMapping("/clientes-pendientes")
    public ResponseEntity<PanelItem> clientesPendientes() {
        Panel panel = panelService.generarPanel();
        PanelItem item = panel.getClientesPendientes();
        return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
    }

    @GetMapping("/pagos-pendientes")
    public ResponseEntity<PanelItem> pagosPendientes() {
        Panel panel = panelService.generarPanel();
        PanelItem item = panel.getPagoPendientes();
        return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
    }

    @GetMapping("/resumen")
    public ResponseEntity<Double> resumen() {
        return ResponseEntity.ok(panelService.generarPanel().getResumen());
    }
}
