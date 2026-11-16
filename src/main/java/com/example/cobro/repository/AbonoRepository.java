package com.example.cobro.repository;

import com.example.cobro.model.Abono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AbonoRepository extends JpaRepository<Abono, Long> {

    List<Abono> findByPrestamo_IdOrderByFechaRegistroAsc(Long prestamoId);

    List<Abono> findByPrestamo_Cliente_IdOrderByFechaRegistroAsc(Long clienteId);

    List<Abono> findByCobrador_IdAndFechaOrderByFechaRegistroAsc(Long cobradorId, LocalDate fecha);

    List<Abono> findByFechaOrderByFechaRegistroDesc(LocalDate fecha);

    boolean existsByPrestamo_IdAndFecha(Long prestamoId, LocalDate fecha);

    @Query("SELECT COALESCE(SUM(a.monto), 0) FROM Abono a WHERE a.prestamo.id = :prestamoId")
    Double sumMontoByPrestamo(@Param("prestamoId") Long prestamoId);

    @Query("SELECT COALESCE(SUM(a.monto), 0) FROM Abono a WHERE a.cobrador.id = :cobradorId AND a.fecha = :fecha")
    Double sumMontoByCobradorAndFecha(@Param("cobradorId") Long cobradorId, @Param("fecha") LocalDate fecha);

    void deleteByPrestamo_Id(Long prestamoId);
}
