package com.example.cobro.ia;

import com.example.cobro.model.EntrenamientoModelo;
import com.example.cobro.model.PrediccionRiesgo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** API del módulo de Machine Learning (credit scoring). */
@RestController
@RequestMapping("/api/ml")
public class MlController {

    @Autowired
    private CreditScoringService creditScoringService;

    @Autowired
    private HistorialMlService historialMl;

    /** Reporte del modelo vigente: métricas en prueba, validación cruzada, ROC y comparación de algoritmos. */
    @GetMapping("/modelo")
    public ResumenModeloDTO modelo() {
        return creditScoringService.getResumen();
    }

    /** Reentrena y compara los algoritmos con la cartera actual. */
    @PostMapping("/entrenar")
    public ResumenModeloDTO entrenar() {
        return creditScoringService.entrenar(EntrenamientoModelo.Origen.MANUAL);
    }

    /** Últimos entrenamientos registrados. */
    @GetMapping("/entrenamientos")
    public List<EntrenamientoModelo> entrenamientos() {
        return historialMl.ultimosEntrenamientos();
    }

    /** Evolución del riesgo de un cliente (predicciones guardadas). */
    @GetMapping("/predicciones/cliente/{clienteId}")
    public List<Prediccion> predicciones(@PathVariable Long clienteId) {
        return historialMl.prediccionesDeCliente(clienteId).stream().map(Prediccion::de).toList();
    }

    /** Simulador: estima el riesgo para un comportamiento de pago hipotético. */
    @PostMapping("/simular")
    public ScoringDTO simular(@Valid @RequestBody Simulacion s) {
        CaracteristicasCrediticias c = new CaracteristicasCrediticias(
                s.promedioDiasAtraso(), Math.max(s.maxDiasAtraso(), s.promedioDiasAtraso()),
                s.tasaCuotasAtrasadas(), s.tasaAbonosIncompletos(), s.frecuenciaPago(),
                s.tasaSaldoVencido(), s.prestamosPagados(), 10);
        return creditScoringService.evaluar(null, 0, c);
    }

    public record Prediccion(LocalDateTime fecha, double probabilidad, String nivelRiesgo,
                             String algoritmo, PrediccionRiesgo.Origen origen) {
        static Prediccion de(PrediccionRiesgo p) {
            return new Prediccion(p.getFecha(), p.getProbabilidad(), p.getNivelRiesgo(), p.getAlgoritmo(), p.getOrigen());
        }
    }

    /** Comportamiento de pago hipotético (mismas variables que usa el modelo). */
    public record Simulacion(
            @NotNull @DecimalMin("0") @DecimalMax("30") Double promedioDiasAtraso,
            @NotNull @DecimalMin("0") @DecimalMax("60") Double maxDiasAtraso,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double tasaCuotasAtrasadas,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double tasaAbonosIncompletos,
            @NotNull @DecimalMin("0") @DecimalMax("1.5") Double frecuenciaPago,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double tasaSaldoVencido,
            @NotNull @DecimalMin("0") @DecimalMax("10") Double prestamosPagados
    ) {}
}
