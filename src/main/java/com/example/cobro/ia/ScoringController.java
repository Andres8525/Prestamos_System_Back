package com.example.cobro.ia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scoring")
public class ScoringController {

    @Autowired
    private CreditScoringService creditScoringService;

    /** Probabilidad de incumplimiento del cliente y recomendación para renovar su crédito. */
    @GetMapping("/cliente/{clienteId}")
    public ScoringDTO evaluarCliente(@PathVariable Long clienteId) {
        return creditScoringService.evaluarCliente(clienteId);
    }

    /** Métricas y pesos del modelo vigente. */
    @GetMapping("/modelo")
    public ScoringDTO.ModeloInfo modelo() {
        return creditScoringService.getInfoModelo();
    }

    /** Reentrena el modelo con la cartera actual. */
    @PostMapping("/entrenar")
    public ScoringDTO.ModeloInfo entrenar() {
        return creditScoringService.entrenar();
    }
}
