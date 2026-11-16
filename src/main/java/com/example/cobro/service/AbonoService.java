package com.example.cobro.service;

import com.example.cobro.dto.AbonoResponseDTO;
import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.*;
import com.example.cobro.repository.AbonoRepository;
import com.example.cobro.repository.CobradorRepository;
import com.example.cobro.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de abonos (pagos diarios en la calle).
 * Un abono se reparte sobre las cuotas pendientes empezando por la más antigua (FIFO);
 * una cuota queda pagada cuando su saldo llega a cero.
 */
@Service
@Transactional
public class AbonoService {

    private static final double TOLERANCIA = 0.009;

    @Autowired
    private AbonoRepository abonoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private CobradorRepository cobradorRepository;

    @Autowired
    private PrestamoService prestamoService;

    /** Registra un abono para la jornada de hoy. */
    public AbonoResponseDTO registrarAbono(Long prestamoId, Double monto, Long cobradorId, String observacion) {
        return registrarAbono(prestamoId, monto, cobradorId, observacion, LocalDate.now());
    }

    public AbonoResponseDTO registrarAbono(Long prestamoId, Double monto, Long cobradorId,
                                           String observacion, LocalDate fecha) {
        Prestamo prestamo = prestamoService.obtenerPrestamoOFallar(prestamoId);
        List<Pago> pendientes = pagoRepository.findByPrestamo_IdAndFechaPagoIsNullOrderByNumeroAsc(prestamoId);
        return aplicar(prestamo, pendientes, monto, cobradorId, observacion, fecha);
    }

    /** Paga el saldo completo de una cuota específica (botón "Registrar pago" de una cuota). */
    public AbonoResponseDTO pagarCuota(Long pagoId, Long cobradorId) {
        Pago cuota = pagoRepository.findById(pagoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", pagoId));
        if (cuota.estaPagada()) {
            throw new ReglaNegocioException("La cuota #" + cuota.getNumero() + " ya fue pagada");
        }
        return aplicar(cuota.getPrestamo(), List.of(cuota), CalculadoraIntereses.redondear(cuota.saldo()),
                cobradorId, "Pago de cuota #" + cuota.getNumero(), LocalDate.now());
    }

    private AbonoResponseDTO aplicar(Prestamo prestamo, List<Pago> cuotasObjetivo, Double monto,
                                     Long cobradorId, String observacion, LocalDate fecha) {
        if (monto == null || monto <= 0) {
            throw new ReglaNegocioException("El monto del abono debe ser mayor a cero");
        }
        if (cuotasObjetivo.isEmpty()) {
            throw new ReglaNegocioException("El préstamo #" + prestamo.getId() + " no tiene saldo pendiente");
        }
        double saldoObjetivo = cuotasObjetivo.stream().mapToDouble(Pago::saldo).sum();
        if (monto > saldoObjetivo + TOLERANCIA) {
            throw new ReglaNegocioException(String.format(
                    "El abono (%.2f) excede el saldo pendiente (%.2f)", monto, saldoObjetivo));
        }

        Cobrador cobrador = resolverCobrador(cobradorId, prestamo);
        double cuotaEsperada = prestamo.getValorCuota() != null
                ? prestamo.getValorCuota() : cuotasObjetivo.get(0).getMonto();

        Abono abono = new Abono();
        abono.setPrestamo(prestamo);
        abono.setCobrador(cobrador);
        abono.setMonto(CalculadoraIntereses.redondear(monto));
        abono.setFecha(fecha);
        abono.setFechaRegistro(LocalDateTime.now());
        abono.setCuotaEsperada(Math.min(cuotaEsperada, saldoObjetivo));
        abono.setObservacion(observacion);
        abono = abonoRepository.save(abono);

        int cubiertas = distribuir(cuotasObjetivo, monto, fecha);
        prestamoService.recalcularEstado(prestamo, fecha);

        AbonoResponseDTO dto = AbonoResponseDTO.fromEntity(abono);
        dto.setCuotasCubiertas(cubiertas);
        dto.setSaldoPrestamo(CalculadoraIntereses.redondear(prestamoService.calcularSaldo(prestamo.getId())));
        return dto;
    }

    /** Reparte el monto sobre las cuotas en orden y devuelve cuántas quedaron totalmente pagadas. */
    private int distribuir(List<Pago> cuotas, double monto, LocalDate fecha) {
        double restante = monto;
        int cubiertas = 0;
        for (Pago cuota : cuotas) {
            if (restante <= TOLERANCIA) break;
            double aplicar = Math.min(restante, cuota.saldo());
            cuota.setMontoPagado(CalculadoraIntereses.redondear(cuota.montoPagadoEfectivo() + aplicar));
            restante -= aplicar;
            if (cuota.getMonto() - cuota.getMontoPagado() <= TOLERANCIA) {
                cuota.setMontoPagado(cuota.getMonto());
                cuota.setFechaPago(fecha);
                cuota.setEstado(PagoService.clasificarPuntualidad(cuota.getFechaProgramada(), fecha));
                cubiertas++;
            }
        }
        pagoRepository.saveAll(cuotas);
        return cubiertas;
    }

    private Cobrador resolverCobrador(Long cobradorId, Prestamo prestamo) {
        if (cobradorId == null) return prestamo.getCobrador();
        return cobradorRepository.findById(cobradorId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cobrador", cobradorId));
    }

    @Transactional(readOnly = true)
    public List<Abono> listarPorPrestamo(Long prestamoId) {
        return abonoRepository.findByPrestamo_IdOrderByFechaRegistroAsc(prestamoId);
    }

    @Transactional(readOnly = true)
    public List<Abono> listarPorCobradorYFecha(Long cobradorId, LocalDate fecha) {
        return abonoRepository.findByCobrador_IdAndFechaOrderByFechaRegistroAsc(cobradorId, fecha);
    }

    @Transactional(readOnly = true)
    public List<Abono> listarPorFecha(LocalDate fecha) {
        return abonoRepository.findByFechaOrderByFechaRegistroDesc(fecha);
    }
}
