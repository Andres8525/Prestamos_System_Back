package com.example.cobro.worker.manejadores;

import com.example.cobro.estructuras.IndiceAgrupado;
import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Pago;
import com.example.cobro.model.Prestamo;
import com.example.cobro.repository.PagoRepository;
import com.example.cobro.repository.PrestamoRepository;
import com.example.cobro.worker.ManejadorTarea;
import com.example.cobro.worker.TipoTarea;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/** Genera un CSV de la cartera activa (saldo, cuotas vencidas, días de atraso y riesgo de cada préstamo). */
@Component
public class ReporteCarteraWorker implements ManejadorTarea {

    static final String CABECERA = "prestamo_id,cliente,cobrador,tipo,monto_total,saldo,cuotas_vencidas,dias_atraso,estado,riesgo";

    private final PrestamoRepository prestamoRepository;
    private final PagoRepository pagoRepository;

    public ReporteCarteraWorker(PrestamoRepository prestamoRepository, PagoRepository pagoRepository) {
        this.prestamoRepository = prestamoRepository;
        this.pagoRepository = pagoRepository;
    }

    @Override
    public TipoTarea tipo() {
        return TipoTarea.REPORTE_CARTERA;
    }

    @Override
    public Resultado ejecutar() {
        LocalDate hoy = LocalDate.now();
        StringBuilder csv = new StringBuilder(CABECERA).append('\n');
        int filas = 0;
        double saldoTotal = 0;
        List<Prestamo> activos = prestamoRepository.findAll().stream()
                .filter(p -> p.getEstado() != EstadoPago.PAGADO).toList();
        IndiceAgrupado<Long, Pago> cuotas = IndiceAgrupado.de(
                pagoRepository.findByPrestamo_IdInAndFechaPagoIsNullOrderByPrestamo_IdAscNumeroAsc(
                        activos.stream().map(Prestamo::getId).toList()),
                Pago::getPrestamoId);
        for (Prestamo p : activos) {
            List<Pago> pendientes = cuotas.de(p.getId());
            double saldo = pendientes.stream().mapToDouble(Pago::saldo).sum();
            long vencidas = pendientes.stream().filter(c -> c.getFechaProgramada().isBefore(hoy)).count();
            long atraso = pendientes.isEmpty() ? 0
                    : Math.max(0, ChronoUnit.DAYS.between(pendientes.get(0).getFechaProgramada(), hoy));
            csv.append(p.getId()).append(',')
               .append(texto(p.getCliente() != null ? p.getCliente().getNombre() : "")).append(',')
               .append(texto(p.getCobrador() != null ? p.getCobrador().getNombre() : "")).append(',')
               .append(p.getTipoLiquidacion()).append(',')
               .append(dinero(p.getMontoTotal())).append(',')
               .append(dinero(saldo)).append(',')
               .append(vencidas).append(',')
               .append(atraso).append(',')
               .append(p.getEstado()).append(',')
               .append(p.getNivelRiesgo() != null ? p.getNivelRiesgo() : "").append('\n');
            filas++;
            saldoTotal += saldo;
        }
        return new Resultado(filas + " préstamos activos, saldo total " + dinero(saldoTotal), csv.toString());
    }

    /** Escapa comillas y separadores según RFC 4180. */
    static String texto(String valor) {
        if (valor == null) return "";
        boolean especial = valor.contains(",") || valor.contains("\"") || valor.contains("\n");
        return especial ? '"' + valor.replace("\"", "\"\"") + '"' : valor;
    }

    private static String dinero(Double valor) {
        return String.format(Locale.ROOT, "%.2f", valor == null ? 0.0 : valor);
    }
}
