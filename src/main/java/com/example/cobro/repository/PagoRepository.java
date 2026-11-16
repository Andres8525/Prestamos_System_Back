package com.example.cobro.repository;

import com.example.cobro.model.EstadoPago;
import com.example.cobro.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cuotas del plan de pagos. Una cuota sigue pendiente de cobro mientras fechaPago sea null;
 * el campo "estado" solo indica la puntualidad (AL_DIA / PENDIENTE / ATRASADO).
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByPrestamo_IdOrderByNumeroAsc(Long prestamoId);

    List<Pago> findByPrestamo_IdAndEstado(Long prestamoId, EstadoPago estado);

    List<Pago> findByEstado(EstadoPago estado);

    long countByEstado(EstadoPago estado);

    List<Pago> findByPrestamo_IdAndFechaPagoIsNullOrderByNumeroAsc(Long prestamoId);

    Optional<Pago> findFirstByPrestamo_IdAndFechaPagoIsNullOrderByNumeroAsc(Long prestamoId);

    /** Cuotas pendientes de varios préstamos en una sola consulta (evita N+1). */
    List<Pago> findByPrestamo_IdInAndFechaPagoIsNullOrderByPrestamo_IdAscNumeroAsc(java.util.Collection<Long> prestamoIds);

    long countByFechaPagoIsNull();

    List<Pago> findByFechaPagoIsNullAndFechaProgramadaBeforeOrderByFechaProgramadaAsc(LocalDate fecha);

    List<Pago> findByPrestamo_IdAndFechaPagoIsNotNull(Long prestamoId);

    @Query("SELECT COUNT(p) FROM Pago p WHERE p.fechaPago IS NOT NULL " +
           "AND YEAR(p.fechaPago) = :year AND MONTH(p.fechaPago) = :month")
    long countRealizadosByMes(@Param("year") int year, @Param("month") int month);
}
