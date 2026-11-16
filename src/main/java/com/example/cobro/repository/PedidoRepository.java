package com.example.cobro.repository;

import com.example.cobro.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findAllByOrderByFechaDesc();
    List<Pedido> findByCliente_IdOrderByFechaDesc(Long clienteId);
    List<Pedido> findByPrestamo_Id(Long prestamoId);
    boolean existsByCliente_Id(Long clienteId);
}
