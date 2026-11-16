package com.example.cobro.service;

import com.example.cobro.dto.PrestamoRequestDTO;
import com.example.cobro.dto.SimulacionPrestamoDTO;
import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.exception.RiesgoCreditoException;
import com.example.cobro.ia.CreditScoringService;
import com.example.cobro.ia.NivelRiesgo;
import com.example.cobro.ia.ScoringDTO;
import com.example.cobro.repository.CobradorRepository;
import com.example.cobro.model.*;
import com.example.cobro.repository.AbonoRepository;
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

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private CobradorRepository cobradorRepository;

    @Autowired
    private AbonoRepository abonoRepository;

    @Autowired
    private CajaService cajaService;

    @Autowired
    private CreditScoringService creditScoringService;

    public Prestamo crearPrestamo(Long clienteId, Double monto, TipoLiquidacion tipo) {
        return crearPrestamo(new PrestamoRequestDTO(clienteId, monto, tipo), LocalDate.now());
    }

    /** Cálculo automático de intereses y plan de cuotas, sin persistir nada. */
    public SimulacionPrestamoDTO simular(PrestamoRequestDTO dto, LocalDate inicio) {
        PlanLiquidacion plan = PlanLiquidacion.de(dto.getTipoLiquidacion());
        double tasa = dto.getTasaInteres() != null ? dto.getTasaInteres() : CalculadoraIntereses.TASA_POR_DEFECTO;
        int numeroCuotas = dto.getNumeroCuotas() != null ? dto.getNumeroCuotas() : plan.cuotasPorDefecto();
        double montoTotal = CalculadoraIntereses.calcularMontoTotal(dto.getMonto(), tasa);
        return new SimulacionPrestamoDTO(
                dto.getMonto(), tasa, CalculadoraIntereses.calcularInteres(dto.getMonto(), tasa), montoTotal,
                plan.getTipo(), numeroCuotas, CalculadoraIntereses.calcularValorCuota(montoTotal, numeroCuotas),
                inicio, plan.fechaVencimiento(inicio, numeroCuotas));
    }

    public Prestamo crearPrestamo(PrestamoRequestDTO dto, LocalDate fechaInicio) {
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", dto.getClienteId()));

        SimulacionPrestamoDTO calculo = simular(dto, fechaInicio);

        // IA: antes de aprobar una RENOVACIÓN se evalúa el riesgo de incumplimiento del cliente.
        boolean esRenovacion = !prestamoRepository.findByCliente_Id(cliente.getId()).isEmpty();
        ScoringDTO scoring = creditScoringService.evaluarCliente(cliente.getId());
        boolean riesgoAlto = scoring.nivelRiesgo() == NivelRiesgo.ALTO;
        if (esRenovacion && riesgoAlto && !dto.isForzarAprobacion()) {
            throw new RiesgoCreditoException(scoring);
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setCliente(cliente);
        prestamo.setCobrador(resolverCobrador(dto.getCobradorId(), cliente));
        // El desembolso sale de la caja del cobrador: no se permite si ya cuadró ese día.
        cajaService.verificarCajaAbierta(prestamo.getCobrador(), fechaInicio);
        prestamo.setMonto(calculo.monto());
        prestamo.setTasaInteres(calculo.tasaInteres());
        prestamo.setMontoTotal(calculo.montoTotal());
        prestamo.setTipoLiquidacion(calculo.tipoLiquidacion());
        prestamo.setNumeroCuotas(calculo.numeroCuotas());
        prestamo.setValorCuota(calculo.valorCuota());
        prestamo.setFechaInicio(fechaInicio);
        prestamo.setFechaVencimiento(calculo.fechaVencimiento());
        prestamo.setEstado(EstadoPago.AL_DIA);
        prestamo.setProbabilidadDefault(scoring.probabilidadDefault());
        prestamo.setNivelRiesgo(scoring.nivelRiesgo().name());
        prestamo.setAprobadoConAlerta(esRenovacion && riesgoAlto);

        Prestamo guardado = prestamoRepository.save(prestamo);
        generarCuotas(guardado, PlanLiquidacion.de(calculo.tipoLiquidacion()),
                calculo.montoTotal(), calculo.numeroCuotas(), fechaInicio);

        return guardado;
    }

    private void generarCuotas(Prestamo prestamo, PlanLiquidacion plan, double montoTotal,
                               int numeroCuotas, LocalDate inicio) {
        List<Pago> cuotas = new ArrayList<>();
        List<Double> montos = CalculadoraIntereses.distribuirCuotas(montoTotal, numeroCuotas);
        for (int i = 1; i <= numeroCuotas; i++) {
            Pago pago = new Pago();
            pago.setPrestamo(prestamo);
            pago.setNumero(i);
            pago.setMonto(montos.get(i - 1));
            pago.setFechaProgramada(plan.fechaCuota(inicio, i));
            pago.setEstado(EstadoPago.AL_DIA);
            cuotas.add(pago);
        }
        pagoRepository.saveAll(cuotas);
    }

    private Cobrador resolverCobrador(Long cobradorId, Cliente cliente) {
        if (cobradorId == null) return cliente.getCobrador();
        Cobrador cobrador = cobradorRepository.findById(cobradorId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cobrador", cobradorId));
        if (!cobrador.isActivo()) {
            throw new ReglaNegocioException("El cobrador " + cobrador.getNombre() + " está inactivo");
        }
        return cobrador;
    }

    @Transactional(readOnly = true)
    public List<Prestamo> listarPorCobrador(Long cobradorId) {
        return prestamoRepository.findByCobrador_Id(cobradorId);
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
        abonoRepository.deleteByPrestamo_Id(id);
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
            nuevoEstado = EstadoPago.PAGADO;
        } else {
            long diasAtraso = ChronoUnit.DAYS.between(proximoPago.get().getFechaProgramada(), hoy);
            nuevoEstado = PlanLiquidacion.de(prestamo.getTipoLiquidacion()).clasificar(diasAtraso);
        }
        prestamo.setEstado(nuevoEstado);
        return prestamoRepository.save(prestamo);
    }

    /** Saldo pendiente del préstamo (suma de los saldos de sus cuotas). */
    @Transactional(readOnly = true)
    public double calcularSaldo(Long prestamoId) {
        return pagoRepository.findByPrestamo_IdAndFechaPagoIsNullOrderByNumeroAsc(prestamoId).stream()
                .mapToDouble(Pago::saldo).sum();
    }

    public void actualizarEstados() {
        LocalDate hoy = LocalDate.now();
        prestamoRepository.findAll().forEach(p -> recalcularEstado(p, hoy));
    }
}
