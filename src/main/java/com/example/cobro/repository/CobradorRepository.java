package com.example.cobro.repository;

import com.example.cobro.model.Cobrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CobradorRepository extends JpaRepository<Cobrador, Long> {
    boolean existsByDocumento(String documento);
    List<Cobrador> findByActivoTrueOrderByNombreAsc();
}
