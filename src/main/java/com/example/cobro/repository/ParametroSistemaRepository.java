package com.example.cobro.repository;

import com.example.cobro.model.ParametroSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, String> {
}
