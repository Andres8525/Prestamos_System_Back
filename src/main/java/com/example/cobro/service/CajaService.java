package com.example.cobro.service;

import com.example.cobro.dto.AbonoResponseDTO;
import com.example.cobro.dto.CierreCajaRequestDTO;
import com.example.cobro.dto.CuadreCajaDTO;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.Abono;
import com.example.cobro.model.Cobrador;
import com.example.cobro.model.CuadreCaja;
import com.example.cobro.model.Prestamo;
import com.example.cobro.repository.AbonoRepository;
import com.example.cobro.repository.CuadreCajaRepository;
import com.example.cobro.repository.PrestamoRepository;
import com.example.cobro.tiemporeal.EventoNegocio;
import com.example.cobro.tiemporeal.TipoEvento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.cobro.service.CalculadoraIntereses.redondear;

/**
 * Balance de caja (cuadre) diario por agente cobrador.
 * El cobrador sale con una base, recauda abonos, desembolsa préstamos nuevos y puede tener gastos;
 * al final del día entrega el efectivo y el sistema calcula el sobrante o faltante.
 */
@Service
@Transactional
public class CajaService {

    @Autowired
    private ApplicationEventPublisher eventos;

    @Autowired
    private CobradorService cobradorService;

    @Autowired
    private AbonoRepository abonoRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private CuadreCajaRepository cuadreCajaRepository;

    /** Devuelve el cuadre cerrado del día o, si no existe, la vista previa con los movimientos actuales. */
    @Transactional(readOnly = true)
    public CuadreCajaDTO consultar(Long cobradorId, LocalDate fecha, Double baseInicial, Double gastos) {
        Cobrador cobrador = cobradorService.obtener(cobradorId);
        List<Abono> abonos = abonoRepository.findByCobrador_IdAndFechaOrderByFechaRegistroAsc(cobradorId, fecha);
        List<Prestamo> desembolsos = prestamoRepository.findByCobrador_IdAndFechaInicio(cobradorId, fecha);

        return cuadreCajaRepository.findByCobrador_IdAndFecha(cobradorId, fecha)
                .map(c -> toDto(c, abonos, desembolsos))
                .orElseGet(() -> vistaPrevia(cobrador, fecha, abonos, desembolsos,
                        baseInicial != null ? baseInicial : cobrador.getCajaBase(),
                        gastos != null ? gastos : 0.0));
    }

    public CuadreCajaDTO cerrar(CierreCajaRequestDTO dto) {
        LocalDate fecha = dto.getFecha() != null ? dto.getFecha() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede cerrar la caja de una fecha futura");
        }
        if (cuadreCajaRepository.existsByCobrador_IdAndFecha(dto.getCobradorId(), fecha)) {
            throw new ReglaNegocioException("La caja del " + fecha + " ya fue cerrada para este cobrador");
        }
        Cobrador cobrador = cobradorService.obtener(dto.getCobradorId());
        List<Abono> abonos = abonoRepository.findByCobrador_IdAndFechaOrderByFechaRegistroAsc(cobrador.getId(), fecha);
        List<Prestamo> desembolsos = prestamoRepository.findByCobrador_IdAndFechaInicio(cobrador.getId(), fecha);

        double base = dto.getBaseInicial() != null ? dto.getBaseInicial() : cobrador.getCajaBase();
        double gastos = dto.getGastos() != null ? dto.getGastos() : 0.0;
        double cobrado = sumarAbonos(abonos);
        double prestado = sumarDesembolsos(desembolsos);
        double esperado = calcularEfectivoEsperado(base, cobrado, prestado, gastos);

        CuadreCaja cuadre = new CuadreCaja();
        cuadre.setCobrador(cobrador);
        cuadre.setFecha(fecha);
        cuadre.setBaseInicial(redondear(base));
        cuadre.setTotalCobrado(cobrado);
        cuadre.setNumeroAbonos(abonos.size());
        cuadre.setTotalPrestado(prestado);
        cuadre.setNumeroPrestamos(desembolsos.size());
        cuadre.setGastos(redondear(gastos));
        cuadre.setEfectivoEsperado(esperado);
        cuadre.setEfectivoEntregado(redondear(dto.getEfectivoEntregado()));
        cuadre.setDiferencia(redondear(dto.getEfectivoEntregado() - esperado));
        cuadre.setObservacion(dto.getObservacion());
        cuadre.setFechaCierre(LocalDateTime.now());

        CuadreCajaDTO resultado = toDto(cuadreCajaRepository.save(cuadre), abonos, desembolsos);
        eventos.publishEvent(EventoNegocio.de(TipoEvento.CAJA_CERRADA,
                "Caja cerrada: " + cobrador.getNombre() + " (" + resultado.resultado() + ")",
                "cobradorId", cobrador.getId(), "fecha", fecha.toString(), "totalCobrado", cobrado,
                "diferencia", resultado.diferencia(), "resultado", resultado.resultado()));
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<CuadreCajaDTO> historial(Long cobradorId) {
        cobradorService.obtener(cobradorId);
        return cuadreCajaRepository.findByCobrador_IdOrderByFechaDesc(cobradorId).stream()
                .map(c -> toDto(c, null, null)).toList();
    }

    /** Impide registrar movimientos en una caja ya cerrada. */
    @Transactional(readOnly = true)
    public void verificarCajaAbierta(Cobrador cobrador, LocalDate fecha) {
        if (cobrador != null && cuadreCajaRepository.existsByCobrador_IdAndFecha(cobrador.getId(), fecha)) {
            throw new ReglaNegocioException("La caja de " + cobrador.getNombre() + " del " + fecha
                    + " ya está cerrada; no se pueden registrar más movimientos ese día");
        }
    }

    public static double calcularEfectivoEsperado(double base, double cobrado, double prestado, double gastos) {
        return redondear(base + cobrado - prestado - gastos);
    }

    private double sumarAbonos(List<Abono> abonos) {
        return redondear(abonos.stream().mapToDouble(Abono::getMonto).sum());
    }

    private double sumarDesembolsos(List<Prestamo> prestamos) {
        return redondear(prestamos.stream().mapToDouble(Prestamo::getMonto).sum());
    }

    private CuadreCajaDTO vistaPrevia(Cobrador cobrador, LocalDate fecha, List<Abono> abonos,
                                     List<Prestamo> desembolsos, double base, double gastos) {
        double cobrado = sumarAbonos(abonos);
        double prestado = sumarDesembolsos(desembolsos);
        return new CuadreCajaDTO(null, cobrador.getId(), cobrador.getNombre(), cobrador.getZona(), fecha,
                CuadreCajaDTO.ABIERTA, redondear(base), cobrado, abonos.size(), prestado, desembolsos.size(),
                redondear(gastos), calcularEfectivoEsperado(base, cobrado, prestado, gastos),
                null, null, null, null, null,
                abonos.stream().map(AbonoResponseDTO::fromEntity).toList(),
                desembolsos.stream().map(this::toDesembolso).toList());
    }

    private CuadreCajaDTO toDto(CuadreCaja c, List<Abono> abonos, List<Prestamo> desembolsos) {
        return new CuadreCajaDTO(c.getId(), c.getCobrador().getId(), c.getCobrador().getNombre(),
                c.getCobrador().getZona(), c.getFecha(), CuadreCajaDTO.CERRADA,
                c.getBaseInicial(), c.getTotalCobrado(), c.getNumeroAbonos(), c.getTotalPrestado(),
                c.getNumeroPrestamos(), c.getGastos(), c.getEfectivoEsperado(), c.getEfectivoEntregado(),
                c.getDiferencia(), CuadreCajaDTO.resultadoDe(c.getDiferencia()), c.getObservacion(),
                c.getFechaCierre(),
                abonos != null ? abonos.stream().map(AbonoResponseDTO::fromEntity).toList() : List.of(),
                desembolsos != null ? desembolsos.stream().map(this::toDesembolso).toList() : List.of());
    }

    private CuadreCajaDTO.Desembolso toDesembolso(Prestamo p) {
        return new CuadreCajaDTO.Desembolso(p.getId(),
                p.getCliente() != null ? p.getCliente().getNombre() : null, p.getMonto());
    }
}
