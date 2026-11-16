package com.example.cobro.service;

import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.model.*;
import com.example.cobro.repository.ClienteRepository;
import com.example.cobro.repository.PagoRepository;
import com.example.cobro.repository.PrestamoRepository;
import com.example.cobro.service.plan.PlanLiquidacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PrestamoService {

    private static final double TASA_INTERES = 20.0;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PagoRepository pagoRepository;

    public Prestamo crearPrestamo(Long clienteId, Double monto, TipoLiquidacion tipo) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", clienteId));

        PlanLiquidacion plan = PlanLiquidacion.de(tipo);
        int numeroCuotas = plan.cuotasPorDefecto();
        double montoTotal = monto * (1 + TASA_INTERES / 100);
        LocalDate hoy = LocalDate.now();

        Prestamo prestamo = new Prestamo();
        prestamo.setCliente(cliente);
        prestamo.setMonto(monto);
        prestamo.setTasaInteres(TASA_INTERES);
        prestamo.setMontoTotal(montoTotal);
        prestamo.setTipoLiquidacion(tipo);
        prestamo.setFechaInicio(hoy);
        prestamo.setFechaVencimiento(plan.fechaVencimiento(hoy, numeroCuotas));
        prestamo.setEstado(EstadoPago.AL_DIA);

        Prestamo guardado = prestamoRepository.save(prestamo);
        generarCuotas(guardado, plan, montoTotal, numeroCuotas, hoy);

        return guardado;
    }

    private void generarCuotas(Prestamo prestamo, PlanLiquidacion plan, double montoTotal,
                               int numeroCuotas, LocalDate inicio) {
        List<Pago> cuotas = new ArrayList<>();
        double montoCuota = montoTotal / numeroCuotas;
        for (int i = 1; i <= numeroCuotas; i++) {
            Pago pago = new Pago();
            pago.setPrestamo(prestamo);
            pago.setNumero(i);
            pago.setMonto(montoCuota);
            pago.setFechaProgramada(plan.fechaCuota(inicio, i));
            pago.setEstado(EstadoPago.AL_DIA);
            cuotas.add(pago);
        }
        pagoRepository.saveAll(cuotas);
    }

    @Transactional(readOnly = true)
    public Optional<Prestamo> obtenerPrestamo(Long id) {
        return prestamoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Prestamo obtenerPrestamoOFallar(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Préstamo", id));
    }

    @Transactional(readOnly = true)
    public List<Prestamo> listarPrestamos() {
        return prestamoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> listarPorCliente(Long clienteId) {
        return prestamoRepository.findByCliente_Id(clienteId);
    }

    @Transactional(readOnly = true)
    public List<Prestamo> listarPorEstado(EstadoPago estado) {
        return prestamoRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public Double obtenerTotalMes(int year, int month) {
        return prestamoRepository.sumMontoTotalByMes(year, month);
    }

    public Prestamo actualizarPrestamo(Long id, Prestamo datos) {
        Prestamo prestamo = obtenerPrestamoOFallar(id);
        if (datos.getEstado() != null) prestamo.setEstado(datos.getEstado());
        return prestamoRepository.save(prestamo);
    }

    public void eliminarPrestamo(Long id) {
        Prestamo prestamo = obtenerPrestamoOFallar(id);
        pagoRepository.deleteAll(pagoRepository.findByPrestamo_IdOrderByNumeroAsc(id));
        prestamoRepository.delete(prestamo);
    }

    /**
     * Recalcula el estado de un préstamo según los días de atraso de la cuota impaga más antigua,
     * usando la tolerancia definida por su plan de liquidación.
     */
    public Prestamo recalcularEstado(Prestamo prestamo, LocalDate hoy) {
        Optional<Pago> proximoPago = pagoRepository
                .findFirstByPrestamo_IdAndFechaPagoIsNullOrderByNumeroAsc(prestamo.getId());

        EstadoPago nuevoEstado;
        if (proximoPago.isEmpty()) {
            nuevoEstado = EstadoPago.AL_DIA;
        } else {
            long diasAtraso = ChronoUnit.DAYS.between(proximoPago.get().getFechaProgramada(), hoy);
            nuevoEstado = PlanLiquidacion.de(prestamo.getTipoLiquidacion()).clasificar(diasAtraso);
        }
        prestamo.setEstado(nuevoEstado);
        return prestamoRepository.save(prestamo);
    }

    public void actualizarEstados() {
        LocalDate hoy = LocalDate.now();
        prestamoRepository.findAll().forEach(p -> recalcularEstado(p, hoy));
    }
}
