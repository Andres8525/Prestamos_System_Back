package com.example.cobro.config;

import com.example.cobro.dto.AuthDTO;
import com.example.cobro.dto.CierreCajaRequestDTO;
import com.example.cobro.dto.CobradorDTO;
import com.example.cobro.dto.CuadreCajaDTO;
import com.example.cobro.dto.PrestamoRequestDTO;
import com.example.cobro.model.*;
import com.example.cobro.repository.AdministradorRepository;
import com.example.cobro.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import com.example.cobro.repository.CobradorRepository;
import com.example.cobro.repository.PagoRepository;
import com.example.cobro.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Carga una cartera de demostración (solo si no hay cobradores registrados):
 * 3 cobradores con su ruta, 15 clientes con perfiles de pago distintos (buen pagador,
 * irregular y moroso), préstamos históricos con sus abonos diarios y renovaciones vigentes.
 * Este historial es el que usa el modelo de scoring para aprender.
 */
@Component
@org.springframework.core.annotation.Order(10)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    enum Perfil { BUENO, IRREGULAR, MOROSO }

    @Autowired private CobradorRepository cobradorRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private CobradorService cobradorService;
    @Autowired private ClienteService clienteService;
    @Autowired private PrestamoService prestamoService;
    @Autowired private AbonoService abonoService;
    @Autowired private CajaService cajaService;
    @Autowired private AdministradorRepository administradorRepository;
    @Autowired private AuthService authService;

    /** Contraseña del usuario de demostración; vacía = no se crea (valor por defecto en producción). */
    @Value("${app.seed.admin-password:}")
    private String claveAdminDemo;

    private final Random rnd = new Random(7);

    @Override
    public void run(String... args) {
        sembrarAdminDemo();
        if (cobradorRepository.count() > 0) return;
        log.info("Cargando cartera de demostración...");
        LocalDate hoy = LocalDate.now();

        String[][] cobradores = {
                {"Carlos Mendoza", "1700000001", "Ruta Norte", "0991000001"},
                {"Lucía Paredes", "1700000002", "Ruta Centro", "0991000002"},
                {"Jorge Villacís", "1700000003", "Ruta Sur", "0991000003"}
        };
        String[][] clientes = {
                {"María Guamán", "Mercado Norte, puesto 12"},
                {"Pedro Chicaiza", "Av. Real Audiencia y Diego de Vásquez"},
                {"Rosa Quishpe", "Tienda La Esquina, Cotocollao"},
                {"Luis Andrango", "Taller El Pana, Carcelén"},
                {"Ana Toapanta", "Venta de jugos, Parque Kennedy"},
                {"José Simbaña", "Ferretería San José, La Marín"},
                {"Carmen Tipán", "Comedor Doña Carmen, San Roque"},
                {"Miguel Caiza", "Kiosko de revistas, Plaza Grande"},
                {"Gloria Pilataxi", "Bazar Gloria, La Ronda"},
                {"Fernando Lema", "Panadería El Trigal, El Tejar"},
                {"Patricia Yánez", "Peluquería Paty, Chillogallo"},
                {"Hugo Cando", "Venta de frutas, Mercado Mayorista"},
                {"Silvia Morocho", "Costurera, Solanda sector 3"},
                {"Diego Cuichán", "Lavadora de autos, Quitumbe"},
                {"Elena Llumiquinga", "Tienda de abarrotes, La Ecuatoriana"}
        };
        Perfil[] perfiles = {Perfil.BUENO, Perfil.BUENO, Perfil.IRREGULAR, Perfil.MOROSO, Perfil.BUENO};

        for (int c = 0; c < cobradores.length; c++) {
            CobradorDTO dto = new CobradorDTO();
            dto.setNombre(cobradores[c][0]);
            dto.setDocumento(cobradores[c][1]);
            dto.setZona(cobradores[c][2]);
            dto.setTelefono(cobradores[c][3]);
            dto.setCajaBase(300.0);
            Cobrador cobrador = cobradorService.crear(dto);

            for (int k = 0; k < 5; k++) {
                int i = c * 5 + k;
                String cedula = String.format("17%08d", 10000 + i);
                if (clienteRepository.existsByCedula(cedula)) continue;
                Cliente cliente = new Cliente();
                cliente.setCedula(cedula);
                cliente.setNombre(clientes[i][0]);
                cliente.setDireccion(clientes[i][1]);
                cliente.setTelefono(String.format("09%08d", 80000000 + i * 1371));
                cliente.setOrdenRuta(k + 1);
                cliente = clienteService.crearCliente(cliente, cobrador.getId());

                // Perfiles rotados para que cada ruta tenga clientes de todo tipo.
                Perfil perfil = perfiles[(k + c) % perfiles.length];
                sembrarHistorial(cliente, perfil, hoy);
            }
        }

        prestamoService.actualizarEstados();
        cerrarCajasDeAyer(hoy.minusDays(1));
        log.info("Cartera de demostración cargada.");
    }

    private void sembrarAdminDemo() {
        if (claveAdminDemo == null || claveAdminDemo.isBlank() || administradorRepository.count() > 0) return;
        authService.registrar(new AuthDTO.Registro("Administrador Demo", "admin@cobro.com", claveAdminDemo));
        log.info("Usuario de demostración creado: admin@cobro.com");
    }

    private void sembrarHistorial(Cliente cliente, Perfil perfil, LocalDate hoy) {
        // Préstamo histórico: 30 cuotas diarias iniciado hace 75-95 días.
        LocalDate inicio = hoy.minusDays(75 + rnd.nextInt(20));
        Prestamo historico = crear(cliente, 150 + 50 * rnd.nextInt(6), TipoLiquidacion.DIARIO, 30, inicio);
        simularPagos(historico, perfil, inicio, hoy);

        boolean cancelado = prestamoService.calcularSaldo(historico.getId()) <= 0.01;
        if (perfil == Perfil.MOROSO || !cancelado) return;

        // Renovación vigente para quienes cancelaron el anterior.
        LocalDate inicioRenovacion = hoy.minusDays(5 + rnd.nextInt(10));
        TipoLiquidacion tipo = rnd.nextBoolean() ? TipoLiquidacion.DIARIO : TipoLiquidacion.SEMANAL;
        Prestamo vigente = crear(cliente, 200 + 50 * rnd.nextInt(8), tipo,
                tipo == TipoLiquidacion.DIARIO ? 30 : 6, inicioRenovacion);
        simularPagos(vigente, perfil, inicioRenovacion, hoy);
    }

    private Prestamo crear(Cliente cliente, double monto, TipoLiquidacion tipo, int cuotas, LocalDate inicio) {
        PrestamoRequestDTO dto = new PrestamoRequestDTO(cliente.getId(), monto, tipo);
        dto.setNumeroCuotas(cuotas);
        dto.setTasaInteres(20.0);
        dto.setForzarAprobacion(true);
        return prestamoService.crearPrestamo(dto, inicio);
    }

    /** Simula los abonos diarios del cliente desde el inicio del préstamo hasta ayer. */
    private void simularPagos(Prestamo prestamo, Perfil perfil, LocalDate inicio, LocalDate hoy) {
        int diasAbandono = 8 + rnd.nextInt(8); // el moroso deja de pagar a partir de aquí
        for (LocalDate dia = inicio.plusDays(1); dia.isBefore(hoy); dia = dia.plusDays(1)) {
            List<Pago> pendientes = pagoRepository.findByPrestamo_IdAndFechaPagoIsNullOrderByNumeroAsc(prestamo.getId());
            if (pendientes.isEmpty()) return;
            final LocalDate d = dia;
            double exigible = pendientes.stream().filter(p -> !p.getFechaProgramada().isAfter(d))
                    .mapToDouble(Pago::saldo).sum();
            if (exigible <= 0.01) continue;
            double cuota = prestamo.getValorCuota();
            long transcurridos = dia.toEpochDay() - inicio.toEpochDay();

            double monto = switch (perfil) {
                case BUENO -> rnd.nextDouble() < 0.93 ? exigible : 0;
                case IRREGULAR -> {
                    double r = rnd.nextDouble();
                    if (r < 0.30) yield 0;
                    if (r < 0.65) yield cuota * (0.4 + 0.4 * rnd.nextDouble());
                    yield Math.min(exigible, cuota * (1 + rnd.nextInt(2)));
                }
                case MOROSO -> {
                    if (transcurridos > diasAbandono) yield 0;
                    yield rnd.nextDouble() < 0.5 ? cuota * (0.3 + 0.5 * rnd.nextDouble()) : 0;
                }
            };
            double saldo = pendientes.stream().mapToDouble(Pago::saldo).sum();
            monto = Math.min(CalculadoraIntereses.redondear(monto), CalculadoraIntereses.redondear(saldo));
            if (monto >= 0.01) {
                abonoService.registrarAbono(prestamo.getId(), monto, null, null, dia);
            }
        }
    }

    private void cerrarCajasDeAyer(LocalDate ayer) {
        for (Cobrador cobrador : cobradorService.listarActivos()) {
            CuadreCajaDTO previo = cajaService.consultar(cobrador.getId(), ayer, null, null);
            CierreCajaRequestDTO cierre = new CierreCajaRequestDTO();
            cierre.setCobradorId(cobrador.getId());
            cierre.setFecha(ayer);
            cierre.setGastos(5.0);
            // Un cobrador entrega con un pequeño faltante para ilustrar el cuadre.
            double faltante = cobrador.getZona().contains("Sur") ? 2.5 : 0.0;
            cierre.setEfectivoEntregado(CalculadoraIntereses.redondear(previo.efectivoEsperado() - 5.0 - faltante));
            cierre.setObservacion("Cierre generado con los datos de demostración");
            cajaService.cerrar(cierre);
        }
    }
}
