package com.example.cobro.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Abono: dinero recibido por un cobrador en terreno para un préstamo.
 * Puede ser parcial (menor a la cuota), exacto o adelantar varias cuotas;
 * se aplica a las cuotas pendientes en orden (FIFO).
 */
@Entity
@Table(name = "abonos", indexes = {
        @Index(name = "idx_abono_cobrador_fecha", columnList = "cobrador_id, fecha"),
        @Index(name = "idx_abono_prestamo", columnList = "prestamo_id")
})
public class Abono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cobrador_id")
    private Cobrador cobrador;

    @Column(nullable = false)
    private Double monto;

    /** Día de cobro (jornada a la que pertenece el abono para el cuadre de caja). */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    /** Valor de la cuota esperada en el momento del abono (para medir pagos incompletos). */
    private Double cuotaEsperada;

    private String observacion;

    public Abono() {}

    /** Un abono es incompleto si cubre menos que la cuota esperada. */
    public boolean esIncompleto() {
        return cuotaEsperada != null && monto + 0.009 < cuotaEsperada;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }

    public Cobrador getCobrador() { return cobrador; }
    public void setCobrador(Cobrador cobrador) { this.cobrador = cobrador; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Double getCuotaEsperada() { return cuotaEsperada; }
    public void setCuotaEsperada(Double cuotaEsperada) { this.cuotaEsperada = cuotaEsperada; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
