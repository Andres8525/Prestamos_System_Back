package com.example.cobro.service;

import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Pago;
import com.example.cobro.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PagoService {

    /** Días de gracia antes de considerar una cuota como ATRASADA. */
    public static final int DIAS_GRACIA = 3;

    @Autowired
    private PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public List<Pago> listarPagos() {
        return pagoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Pago> obtenerPago(Long id) {
        return pagoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPorPrestamo(Long prestamoId) {
        return pagoRepository.findByPrestamo_IdOrderByNumeroAsc(prestamoId);
    }

    /** Cuotas que aún no se han pagado. */
    @Transactional(readOnly = true)
    public List<Pago> listarPendientesPorPrestamo(Long prestamoId) {
        return pagoRepository.findByPrestamo_IdAndFechaPagoIsNullOrderByNumeroAsc(prestamoId);
    }

    @Transactional(readOnly = true)
    public List<Pago> listarRealizadosPorPrestamo(Long prestamoId) {
        return pagoRepository.findByPrestamo_IdAndFechaPagoIsNotNull(prestamoId);
    }

    /** Cuotas vencidas que siguen sin pagarse. */
    @Transactional(readOnly = true)
    public List<Pago> listarAtrasados() {
        return pagoRepository
                .findByFechaPagoIsNullAndFechaProgramadaBeforeOrderByFechaProgramadaAsc(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        return pagoRepository.countByFechaPagoIsNull();
    }

    /** Clasifica la puntualidad de un pago según los días transcurridos desde la fecha programada. */
    public static EstadoPago clasificarPuntualidad(LocalDate fechaProgramada, LocalDate fechaPago) {
        long diasAtraso = ChronoUnit.DAYS.between(fechaProgramada, fechaPago);
        if (diasAtraso <= 0) return EstadoPago.AL_DIA;
        if (diasAtraso <= DIAS_GRACIA) return EstadoPago.PENDIENTE;
        return EstadoPago.ATRASADO;
    }

    public Pago marcarPagoRealizado(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", id));
        if (pago.getFechaPago() != null) {
            throw new ReglaNegocioException("La cuota #" + pago.getNumero() + " ya fue pagada");
        }

        LocalDate hoy = LocalDate.now();
        pago.setFechaPago(hoy);
        pago.setMontoPagado(pago.getMonto());
        pago.setEstado(clasificarPuntualidad(pago.getFechaProgramada(), hoy));
        return pagoRepository.save(pago);
    }

    public Pago actualizarPago(Long id, Pago datos) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", id));
        if (datos.getEstado() != null) pago.setEstado(datos.getEstado());
        pago.setFechaPago(datos.getFechaPago());
        return pagoRepository.save(pago);
    }

    public void eliminarPago(Long id) {
        if (!pagoRepository.existsById(id)) {
            throw RecursoNoEncontradoException.de("Pago", id);
        }
        pagoRepository.deleteById(id);
    }
}
