package com.example.cobro.repository;

import com.example.cobro.model.TareaProceso;
import com.example.cobro.worker.EstadoTarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TareaProcesoRepository extends JpaRepository<TareaProceso, Long> {

    List<TareaProceso> findTop50ByOrderByIdDesc();

    List<TareaProceso> findByEstadoInOrderByPrioridadDescIdAsc(Collection<EstadoTarea> estados);

    long countByEstado(EstadoTarea estado);
}
