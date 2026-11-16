package com.example.cobro.repository;

import com.example.cobro.model.EntrenamientoModelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EntrenamientoModeloRepository extends JpaRepository<EntrenamientoModelo, Long> {
    List<EntrenamientoModelo> findTop30ByOrderByFechaDesc();
}
