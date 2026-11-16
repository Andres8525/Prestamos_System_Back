package com.example.cobro.ia;

import com.example.cobro.model.EntrenamientoModelo;
import com.example.cobro.repository.EntrenamientoModeloRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Guarda el historial del Machine Learning. Usa transacciones propias (REQUIRES_NEW) para que
 * el registro se conserve aunque la operación que lo originó se revierta.
 */
@Service
public class HistorialMlService {

    private static final Logger log = LoggerFactory.getLogger(HistorialMlService.class);

    @Autowired
    private EntrenamientoModeloRepository entrenamientoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEntrenamiento(ResumenModeloDTO resumen, EntrenamientoModelo.Origen origen) {
        try {
            MetricasClasificacion.Resultado prueba = resumen.evaluacionPrueba();
            EntrenamientoModelo e = new EntrenamientoModelo();
            e.setFecha(LocalDateTime.now());
            e.setOrigen(origen);
            e.setAlgoritmo(resumen.modelo().algoritmo());
            e.setMuestrasReales(resumen.modelo().muestrasReales());
            e.setMuestrasSinteticas(resumen.modelo().muestrasSinteticas());
            e.setAucPrueba(prueba.auc());
            e.setExactitudPrueba(prueba.exactitud());
            e.setPrecisionPrueba(prueba.precision());
            e.setSensibilidadPrueba(prueba.sensibilidad());
            e.setF1Prueba(prueba.f1());
            e.setAucValidacion(resumen.validacionCruzada().aucMedia());
            e.setAucDesviacion(resumen.validacionCruzada().aucDesviacion());
            e.setComparacion(resumen.comparacion().stream()
                    .map(c -> c.algoritmo() + "=" + c.aucValidacion())
                    .collect(Collectors.joining("; ")));
            entrenamientoRepository.save(e);
        } catch (Exception ex) {
            // El historial es informativo: un fallo al guardarlo no debe impedir usar el modelo.
            log.warn("No se pudo registrar el entrenamiento del modelo: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<EntrenamientoModelo> ultimosEntrenamientos() {
        return entrenamientoRepository.findTop30ByOrderByFechaDesc();
    }
}
