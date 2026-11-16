package com.example.cobro.repository;

import com.example.cobro.model.PrediccionRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrediccionRiesgoRepository extends JpaRepository<PrediccionRiesgo, Long> {
    List<PrediccionRiesgo> findTop50ByCliente_IdOrderByFechaDesc(Long clienteId);
}
