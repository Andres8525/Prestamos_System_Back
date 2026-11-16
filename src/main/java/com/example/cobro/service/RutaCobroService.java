package com.example.cobro.service;

import com.example.cobro.dto.RutaCobroDTO;
import com.example.cobro.model.*;
import com.example.cobro.repository.AbonoRepository;
import com.example.cobro.repository.PagoRepository;
import com.example.cobro.repository.PrestamoRepository;
import com.example.cobro.estructuras.IndiceAgrupado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Genera la ruta de cobro diaria: los préstamos activos asignados al cobrador,
 * ordenados según el orden de visita del cliente, con lo que se debe cobrar ese día.
 */
@Service
@Transactional(readOnly = true)
public class RutaCobroService {

    @Autowired
    private CobradorService cobradorService;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private AbonoRepository abonoRepository;

    public RutaCobroDTO generarRuta(Long cobradorId, LocalDate fecha) {
        Cobrador cobrador = cobradorService.obtener(cobradorId);

        Map<Long, Double> abonadoPorPrestamo = abonoRepository
                .findByCobrador_IdAndFechaOrderByFechaRegistroAsc(cobradorId, fecha).stream()
                .collect(Collectors.groupingBy(a -> a.getPrestamo().getId(),
                        Collectors.summingDouble(Abono::getMonto)));

        List<Prestamo> prestamos = new ArrayList<>(
                prestamoRepository.findActivosDeRuta(cobradorId, EstadoPago.PAGADO));
        prestamos.sort(Comparator
                .comparing((Prestamo p) -> p.getCliente().getOrdenRuta(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(p -> p.getCliente().getNombre()));

        IndiceAgrupado<Long, Pago> pendientes = IndiceAgrupado.de(
                pagoRepository.findByPrestamo_IdInAndFechaPagoIsNullOrderByPrestamo_IdAscNumeroAsc(
                        prestamos.stream().map(Prestamo::getId).toList()),
                Pago::getPrestamoId);
        List<RutaCobroDTO.Parada> paradas = new ArrayList<>();
        int orden = 1;
        for (Prestamo prestamo : prestamos) {
            paradas.add(construirParada(orden++, prestamo, pendientes.de(prestamo.getId()), fecha,
                    abonadoPorPrestamo.getOrDefault(prestamo.getId(), 0.0)));
        }
        // Abonos de préstamos que ya quedaron cancelados hoy también cuentan como recaudo.
        double recaudado = abonadoPorPrestamo.values().stream().mapToDouble(Double::doubleValue).sum();

        return new RutaCobroDTO(
                cobrador.getId(), cobrador.getNombre(), cobrador.getZona(), fecha,
                paradas.size(),
                (int) paradas.stream().filter(p -> p.abonadoHoy() > 0).count(),
                CalculadoraIntereses.redondear(paradas.stream().mapToDouble(RutaCobroDTO.Parada::montoExigible).sum()),
                CalculadoraIntereses.redondear(recaudado),
                paradas);
    }

    private RutaCobroDTO.Parada construirParada(int orden, Prestamo prestamo, List<Pago> pendientes,
                                                LocalDate fecha, double abonadoHoy) {
        double exigible = pendientes.stream()
                .filter(c -> !c.getFechaProgramada().isAfter(fecha))
                .mapToDouble(Pago::saldo).sum();
        int vencidas = (int) pendientes.stream()
                .filter(c -> c.getFechaProgramada().isBefore(fecha)).count();
        long diasAtraso = pendientes.isEmpty() ? 0
                : Math.max(0, ChronoUnit.DAYS.between(pendientes.get(0).getFechaProgramada(), fecha));
        double saldo = pendientes.stream().mapToDouble(Pago::saldo).sum();
        double valorCuota = prestamo.getValorCuota() != null ? prestamo.getValorCuota()
                : (pendientes.isEmpty() ? 0.0 : pendientes.get(0).getMonto());

        Cliente cliente = prestamo.getCliente();
        return new RutaCobroDTO.Parada(
                orden, cliente.getId(), cliente.getNombre(), cliente.getDireccion(), cliente.getTelefono(),
                prestamo.getId(), prestamo.getTipoLiquidacion(), prestamo.getEstado(),
                CalculadoraIntereses.redondear(valorCuota),
                CalculadoraIntereses.redondear(exigible),
                vencidas, diasAtraso,
                CalculadoraIntereses.redondear(saldo),
                CalculadoraIntereses.redondear(abonadoHoy),
                exigible > 0);
    }
}
