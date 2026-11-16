package com.example.cobro.repository;

import com.example.cobro.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrueOrderByCategoriaAscNombreAsc();
    List<Producto> findAllByOrderByCategoriaAscNombreAsc();
}
