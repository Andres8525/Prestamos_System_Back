package com.example.cobro.repository;

import com.example.cobro.model.CuadreCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuadreCajaRepository extends JpaRepository<CuadreCaja, Long> {

    Optional<CuadreCaja> findByCobrador_IdAndFecha(Long cobradorId, LocalDate fecha);

    boolean existsByCobrador_IdAndFecha(Long cobradorId, LocalDate fecha);

    List<CuadreCaja> findByCobrador_IdOrderByFechaDesc(Long cobradorId);

    List<CuadreCaja> findByFechaOrderByCobrador_NombreAsc(LocalDate fecha);
}
