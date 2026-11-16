package com.example.cobro.service;

import com.example.cobro.dto.*;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.exception.RiesgoCreditoException;
import com.example.cobro.ia.CreditScoringService;
import com.example.cobro.ia.NivelRiesgo;
import com.example.cobro.model.*;
import com.example.cobro.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Flujo completo: cobrador → cliente → préstamo → abonos → ruta → cuadre de caja → scoring. */
@SpringBootTest
class FlujoCobroIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired private CobradorService cobradorService;
    @Autowired private ClienteService clienteService;
    @Autowired private PrestamoService prestamoService;
    @Autowired private AbonoService abonoService;
    @Autowired private RutaCobroService rutaCobroService;
    @Autowired private CajaService cajaService;
    @Autowired private CreditScoringService creditScoringService;
    @Autowired private PagoRepository pagoRepository;

    @Test
    void abonosParcialesSeAplicanEnOrdenYCuadranLaCaja() {
        Cobrador cobrador = nuevoCobrador();
        Cliente cliente = nuevoCliente(cobrador);
        LocalDate hoy = LocalDate.now();

        PrestamoRequestDTO solicitud = new PrestamoRequestDTO(cliente.getId(), 100.0, TipoLiquidacion.DIARIO);
        solicitud.setNumeroCuotas(4);
        Prestamo prestamo = prestamoService.crearPrestamo(solicitud, hoy.minusDays(2));

        assertEquals(120.0, prestamo.getMontoTotal());
        assertEquals(30.0, prestamo.getValorCuota());
        assertEquals(cobrador.getId(), prestamo.getCobrador().getId(), "Hereda el cobrador del cliente");

        AbonoResponseDTO parcial = abonoService.registrarAbono(prestamo.getId(), 20.0, null, null, hoy);
        assertTrue(parcial.isIncompleto());
        assertEquals(0, parcial.getCuotasCubiertas());
        assertEquals(100.0, parcial.getSaldoPrestamo());

        AbonoResponseDTO doble = abonoService.registrarAbono(prestamo.getId(), 40.0, null, null, hoy);
        assertEquals(2, doble.getCuotasCubiertas());
        assertEquals(60.0, doble.getSaldoPrestamo());

        List<Pago> cuotas = pagoRepository.findByPrestamo_IdOrderByNumeroAsc(prestamo.getId());
        assertTrue(cuotas.get(0).estaPagada());
        assertTrue(cuotas.get(1).estaPagada());
        assertFalse(cuotas.get(2).estaPagada());

        assertThrows(ReglaNegocioException.class,
                () -> abonoService.registrarAbono(prestamo.getId(), 500.0, null, null, hoy));

        RutaCobroDTO ruta = rutaCobroService.generarRuta(cobrador.getId(), hoy);
        assertEquals(1, ruta.totalVisitas());
        assertEquals(60.0, ruta.totalRecaudado());

        CierreCajaRequestDTO cierre = new CierreCajaRequestDTO();
        cierre.setCobradorId(cobrador.getId());
        cierre.setBaseInicial(200.0);
        cierre.setGastos(10.0);
        cierre.setEfectivoEntregado(250.0);
        CuadreCajaDTO cuadre = cajaService.cerrar(cierre);
        // 200 base + 60 cobrado - 0 prestado hoy - 10 gastos = 250
        assertEquals(250.0, cuadre.efectivoEsperado());
        assertEquals("CUADRADA", cuadre.resultado());

        assertThrows(ReglaNegocioException.class,
                () -> abonoService.registrarAbono(prestamo.getId(), 10.0, null, null, hoy),
                "Con la caja cerrada no se registran más abonos ese día");

        abonoService.registrarAbono(prestamo.getId(), 60.0, null, null, hoy.plusDays(1));
        assertEquals(EstadoPago.PAGADO, prestamoService.obtenerPrestamoOFallar(prestamo.getId()).getEstado());
    }

    @Test
    void laIaAlertaAntesDeRenovarAUnClienteMoroso() {
        Cobrador cobrador = nuevoCobrador();
        Cliente cliente = nuevoCliente(cobrador);
        LocalDate hoy = LocalDate.now();

        PrestamoRequestDTO primero = new PrestamoRequestDTO(cliente.getId(), 200.0, TipoLiquidacion.DIARIO);
        primero.setNumeroCuotas(20);
        Prestamo prestamo = prestamoService.crearPrestamo(primero, hoy.minusDays(60));
        // Abonó poco y de forma incompleta solo los primeros días.
        for (int d = 1; d <= 4; d++) {
            abonoService.registrarAbono(prestamo.getId(), 3.0, null, null, hoy.minusDays(60 - d));
        }
        creditScoringService.entrenar();

        var scoring = creditScoringService.evaluarCliente(cliente.getId());
        assertEquals(NivelRiesgo.ALTO, scoring.nivelRiesgo());
        assertEquals("RECHAZAR", scoring.recomendacion());

        PrestamoRequestDTO renovacion = new PrestamoRequestDTO(cliente.getId(), 300.0, TipoLiquidacion.DIARIO);
        assertThrows(RiesgoCreditoException.class, () -> prestamoService.crearPrestamo(renovacion, hoy));

        renovacion.setForzarAprobacion(true);
        Prestamo aprobado = prestamoService.crearPrestamo(renovacion, hoy);
        assertTrue(aprobado.getAprobadoConAlerta());
        assertEquals("ALTO", aprobado.getNivelRiesgo());
    }

    private Cobrador nuevoCobrador() {
        int n = SECUENCIA.incrementAndGet();
        CobradorDTO dto = new CobradorDTO();
        dto.setNombre("Cobrador " + n);
        dto.setDocumento("DOC-" + n);
        dto.setZona("Zona " + n);
        dto.setCajaBase(100.0);
        return cobradorService.crear(dto);
    }

    private Cliente nuevoCliente(Cobrador cobrador) {
        int n = SECUENCIA.incrementAndGet();
        Cliente c = new Cliente();
        c.setCedula("CED-" + n);
        c.setNombre("Cliente " + n);
        c.setDireccion("Calle " + n);
        c.setTelefono("0990000000");
        c.setOrdenRuta(1);
        return clienteService.crearCliente(c, cobrador.getId());
    }
}
