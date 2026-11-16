package com.example.cobro.ia;

import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.model.*;
import com.example.cobro.repository.AbonoRepository;
import com.example.cobro.repository.ClienteRepository;
import com.example.cobro.repository.PagoRepository;
import com.example.cobro.repository.PrestamoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.cobro.tiemporeal.EventoNegocio;
import com.example.cobro.tiemporeal.TipoEvento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

/**
 * Scoring de riesgo de crédito (Credit Scoring) con Machine Learning.
 *
 * <p><b>Entrenamiento.</b> Cada préstamo con desenlace conocido genera una muestra:
 * las características se calculan con el comportamiento observado hasta la mitad del plazo
 * y la etiqueta es el desenlace final (1 = incumplimiento: más de {@value #DIAS_DEFAULT} días
 * de mora en alguna cuota; 0 = préstamo cancelado). Como una cartera nueva tiene pocos casos,
 * el conjunto se complementa con una cartera sintética reproducible (semilla fija); las muestras
 * reales pesan {@value #PESO_REAL} veces más. Se comparan varios algoritmos (regresión logística y
 * árbol de decisión) y se usa el de mejor AUC en validación cruzada.</p>
 *
 * <p><b>Predicción.</b> Para un cliente se usan todas sus cuotas y abonos hasta hoy y el modelo
 * devuelve la probabilidad de incumplimiento, el nivel de riesgo y los factores que la explican.</p>
 */
@Service
@Transactional(readOnly = true)
public class CreditScoringService {

    private static final Logger log = LoggerFactory.getLogger(CreditScoringService.class);

    public static final int DIAS_DEFAULT = 30;
    static final double PESO_REAL = 5.0;
    static final int MUESTRAS_SINTETICAS = 800;
    static final long SEMILLA = 2024L;
    static final double FRACCION_PRUEBA = 0.2;
    static final int PLIEGUES = 5;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private AbonoRepository abonoRepository;

    @Autowired
    private HistorialMlService historialMl;

    @Autowired
    private ApplicationEventPublisher eventos;

    private volatile ModeloClasificacion modelo;
    private volatile ScoringDTO.ModeloInfo infoModelo;
    private volatile ResumenModeloDTO resumen;

    @EventListener(ApplicationReadyEvent.class)
    public void entrenarAlIniciar() {
        try {
            entrenar(EntrenamientoModelo.Origen.INICIO);
        } catch (Exception e) {
            log.warn("No se pudo entrenar el modelo de scoring al iniciar: {}", e.getMessage());
        }
    }

    /** Algoritmos candidatos: se entrenan todos y se elige el de mejor AUC en validación cruzada. */
    static List<Supplier<ModeloClasificacion>> candidatos() {
        return List.of(RegresionLogistica::new, ArbolDecision::new);
    }

    /**
     * (Re)entrena el modelo con la cartera actual más la cartera sintética de referencia.
     * <ol>
     *   <li>Separa un 20 % estratificado como conjunto de prueba (datos que el modelo nunca ve).</li>
     *   <li>Evalúa cada algoritmo candidato con validación cruzada de 5 pliegues sobre el 80 % restante.</li>
     *   <li>Elige el de mejor AUC promedio y mide su desempeño en el conjunto de prueba.</li>
     *   <li>Reentrena el elegido con todos los datos y lo pone en producción.</li>
     * </ol>
     */
    public ResumenModeloDTO entrenar() {
        return entrenar(EntrenamientoModelo.Origen.MANUAL);
    }

    public synchronized ResumenModeloDTO entrenar(EntrenamientoModelo.Origen origen) {
        LocalDate hoy = LocalDate.now();
        List<Muestra> reales = construirMuestrasReales(hoy);
        List<Muestra> sinteticas = generarMuestrasSinteticas(MUESTRAS_SINTETICAS, SEMILLA);

        List<Muestra> todas = new ArrayList<>(sinteticas);
        todas.addAll(reales);
        ValidacionCruzada.Division division = ValidacionCruzada.dividir(todas, FRACCION_PRUEBA, SEMILLA);

        List<ResumenModeloDTO.Comparacion> comparacion = new ArrayList<>();
        Supplier<ModeloClasificacion> mejorFabrica = null;
        ValidacionCruzada.Resultado mejorCv = null;
        MetricasClasificacion.Resultado mejorPrueba = null;
        for (Supplier<ModeloClasificacion> fabrica : candidatos()) {
            ValidacionCruzada.Resultado cv =
                    ValidacionCruzada.evaluar(fabrica, division.entrenamiento(), PLIEGUES, SEMILLA);
            ModeloClasificacion candidato = fabrica.get();
            candidato.entrenar(division.entrenamiento());
            MetricasClasificacion.Resultado prueba = MetricasClasificacion.evaluar(candidato, division.prueba());
            comparacion.add(new ResumenModeloDTO.Comparacion(candidato.nombre(), cv.aucMedia(), cv.aucDesviacion(),
                    prueba.auc(), prueba.exactitud(), prueba.f1(), false));
            if (mejorCv == null || cv.aucMedia() > mejorCv.aucMedia()) {
                mejorFabrica = fabrica;
                mejorCv = cv;
                mejorPrueba = prueba;
            }
        }

        ModeloClasificacion produccion = mejorFabrica.get();
        produccion.entrenar(todas);
        String elegido = produccion.nombre();
        comparacion.replaceAll(c -> c.algoritmo().equals(elegido)
                ? new ResumenModeloDTO.Comparacion(c.algoritmo(), c.aucValidacion(), c.aucDesviacion(),
                        c.aucPrueba(), c.exactitudPrueba(), c.f1Prueba(), true)
                : c);

        ScoringDTO.ModeloInfo info = new ScoringDTO.ModeloInfo(
                elegido, reales.size(), sinteticas.size(),
                mejorPrueba.exactitud(), mejorPrueba.auc(), LocalDateTime.now(),
                produccion instanceof RegresionLogistica rl ? porVariable(rl.getPesos()) : null,
                porVariable(produccion.importancia()));
        ResumenModeloDTO resumen = new ResumenModeloDTO(info, division.entrenamiento().size(),
                division.prueba().size(), mejorPrueba, mejorCv, comparacion);

        this.modelo = produccion;
        this.infoModelo = info;
        this.resumen = resumen;
        historialMl.registrarEntrenamiento(resumen, origen);
        eventos.publishEvent(EventoNegocio.de(TipoEvento.MODELO_ENTRENADO,
                "Modelo reentrenado: " + elegido, "algoritmo", elegido, "auc", mejorPrueba.auc(),
                "origen", origen.name()));
        log.info("Modelo de scoring entrenado: {} ({} muestras reales, {} sintéticas), AUC prueba={}, AUC CV={}±{}",
                elegido, reales.size(), sinteticas.size(), mejorPrueba.auc(), mejorCv.aucMedia(), mejorCv.aucDesviacion());
        return resumen;
    }

    private static Map<String, Double> porVariable(double[] valores) {
        Map<String, Double> mapa = new LinkedHashMap<>();
        for (int i = 0; i < valores.length; i++) {
            mapa.put(CaracteristicasCrediticias.NOMBRES[i], redondear(valores[i]));
        }
        return mapa;
    }

    public ResumenModeloDTO getResumen() {
        asegurarModelo();
        return resumen;
    }

    public ScoringDTO.ModeloInfo getInfoModelo() {
        asegurarModelo();
        return infoModelo;
    }

    /** Evalúa el riesgo de incumplimiento de un cliente con todo su historial hasta hoy. */
    public ScoringDTO evaluarCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", clienteId));
        asegurarModelo();

        LocalDate hoy = LocalDate.now();
        List<Prestamo> prestamos = prestamoRepository.findByCliente_Id(clienteId);
        List<Pago> cuotas = new ArrayList<>();
        prestamos.forEach(p -> cuotas.addAll(pagoRepository.findByPrestamo_IdOrderByNumeroAsc(p.getId())));
        List<Abono> abonos = abonoRepository.findByPrestamo_Cliente_IdOrderByFechaRegistroAsc(clienteId);
        int pagados = (int) prestamos.stream().filter(p -> p.getEstado() == EstadoPago.PAGADO).count();

        CaracteristicasCrediticias c = ExtractorCaracteristicas.extraer(cuotas, abonos, pagados, hoy);
        return evaluar(cliente, prestamos.size(), c);
    }

    /** Aplica el modelo a un vector de características ya calculado. */
    public ScoringDTO evaluar(Cliente cliente, int prestamosAnteriores, CaracteristicasCrediticias c) {
        asegurarModelo();
        ModeloClasificacion m = this.modelo;
        double[] x = c.comoVector();
        double p = m.predecir(x);
        NivelRiesgo nivel = NivelRiesgo.desdeProbabilidad(p);
        String mensaje = c.sinHistorial()
                ? "Cliente sin cuotas vencidas: la predicción se basa solo en el perfil promedio de la cartera."
                : nivel.getMensaje();

        return new ScoringDTO(
                cliente != null ? cliente.getId() : null,
                cliente != null ? cliente.getNombre() : null,
                redondear(p),
                (int) Math.round((1 - p) * 1000),
                nivel, nivel.getRecomendacion(), mensaje,
                c.sinHistorial(), prestamosAnteriores, c.cuotasEvaluadas(),
                c.comoMapa(), factores(m, x), infoModelo);
    }

    // ---------------------------------------------------------------- dataset

    /** Construye muestras a partir de los préstamos cuyo desenlace ya se conoce. */
    List<Muestra> construirMuestrasReales(LocalDate hoy) {
        List<Muestra> muestras = new ArrayList<>();
        for (Prestamo prestamo : prestamoRepository.findAll()) {
            List<Pago> cuotas = pagoRepository.findByPrestamo_IdOrderByNumeroAsc(prestamo.getId());
            if (cuotas.isEmpty()) continue;
            Integer etiqueta = etiquetar(cuotas, hoy);
            if (etiqueta == null) continue;

            long plazo = Math.max(2, ChronoUnit.DAYS.between(prestamo.getFechaInicio(), prestamo.getFechaVencimiento()));
            LocalDate corte = prestamo.getFechaInicio().plusDays(plazo / 2);
            List<Abono> abonos = abonoRepository.findByPrestamo_IdOrderByFechaRegistroAsc(prestamo.getId());
            int pagadosAntes = (int) prestamoRepository.findByCliente_Id(prestamo.getCliente().getId()).stream()
                    .filter(o -> !o.getId().equals(prestamo.getId())
                            && o.getEstado() == EstadoPago.PAGADO
                            && o.getFechaVencimiento().isBefore(prestamo.getFechaInicio()))
                    .count();

            CaracteristicasCrediticias c = ExtractorCaracteristicas.extraer(cuotas, abonos, pagadosAntes, corte);
            if (!c.sinHistorial()) muestras.add(new Muestra(c.comoVector(), etiqueta, PESO_REAL));
        }
        return muestras;
    }

    /**
     * 1 si alguna cuota acumuló más de {@value #DIAS_DEFAULT} días de mora, 0 si el préstamo
     * se canceló sin llegar a ese punto, null si aún no hay desenlace.
     */
    static Integer etiquetar(List<Pago> cuotas, LocalDate hoy) {
        boolean incumplio = cuotas.stream().anyMatch(c -> {
            LocalDate fin = c.getFechaPago() != null ? c.getFechaPago() : hoy;
            return ChronoUnit.DAYS.between(c.getFechaProgramada(), fin) > DIAS_DEFAULT;
        });
        if (incumplio) return 1;
        boolean cancelado = cuotas.stream().allMatch(Pago::estaPagada);
        return cancelado ? 0 : null;
    }

    /**
     * Cartera sintética de referencia. Cada cliente tiene un riesgo latente r ∈ [0,1] que determina
     * su comportamiento de pago (con ruido) y su probabilidad real de incumplir.
     */
    static List<Muestra> generarMuestrasSinteticas(int cantidad, long semilla) {
        Random rnd = new Random(semilla);
        List<Muestra> muestras = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            double r = rnd.nextDouble();
            double promedio = acotar(r * 12 + rnd.nextGaussian() * 2, 0, 30);
            double maximo = acotar(promedio * 2 + r * 15 + Math.abs(rnd.nextGaussian() * 3), promedio, 60);
            double tasaAtrasadas = acotar(r * 0.8 + rnd.nextGaussian() * 0.1, 0, 1);
            double tasaIncompletos = acotar(r * 0.7 + rnd.nextGaussian() * 0.12, 0, 1);
            double frecuencia = acotar(1.1 - r * 0.8 + rnd.nextGaussian() * 0.1, 0, 1.5);
            double tasaSaldo = acotar(r * 0.6 + rnd.nextGaussian() * 0.08, 0, 1);
            double pagados = Math.round(acotar((1 - r) * 4 + rnd.nextGaussian(), 0, 10));

            double probabilidadReal = 1.0 / (1.0 + Math.exp(-8 * (r - 0.55)));
            int etiqueta = rnd.nextDouble() < probabilidadReal ? 1 : 0;
            muestras.add(new Muestra(new double[]{
                    promedio, maximo, tasaAtrasadas, tasaIncompletos, frecuencia, tasaSaldo, pagados
            }, etiqueta, 1.0));
        }
        return muestras;
    }

    // ---------------------------------------------------------------- utilidades

    private List<ScoringDTO.Factor> factores(ModeloClasificacion m, double[] x) {
        double[] contrib = m.contribuciones(x);
        List<ScoringDTO.Factor> lista = new ArrayList<>();
        for (int i = 0; i < contrib.length; i++) {
            lista.add(new ScoringDTO.Factor(CaracteristicasCrediticias.NOMBRES[i],
                    CaracteristicasCrediticias.DESCRIPCIONES[i], redondear(x[i]), redondear(contrib[i])));
        }
        lista.sort(Comparator.comparingDouble((ScoringDTO.Factor f) -> Math.abs(f.impacto())).reversed());
        return lista.subList(0, Math.min(4, lista.size()));
    }

    private void asegurarModelo() {
        if (modelo == null) entrenar(EntrenamientoModelo.Origen.INICIO);
    }

    private static double acotar(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double redondear(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
