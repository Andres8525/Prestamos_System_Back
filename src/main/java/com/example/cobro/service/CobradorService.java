package com.example.cobro.service;

import com.example.cobro.dto.CobradorDTO;
import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.Cobrador;
import com.example.cobro.repository.CobradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CobradorService {

    @Autowired
    private CobradorRepository cobradorRepository;

    @Transactional(readOnly = true)
    public List<Cobrador> listar() {
        return cobradorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cobrador> listarActivos() {
        return cobradorRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Cobrador obtener(Long id) {
        return cobradorRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cobrador", id));
    }

    public Cobrador crear(CobradorDTO dto) {
        if (cobradorRepository.existsByDocumento(dto.getDocumento().trim())) {
            throw new ReglaNegocioException("Ya existe un cobrador con el documento: " + dto.getDocumento());
        }
        Cobrador cobrador = new Cobrador();
        dto.aplicarA(cobrador);
        return cobradorRepository.save(cobrador);
    }

    public Cobrador actualizar(Long id, CobradorDTO dto) {
        Cobrador cobrador = obtener(id);
        String documento = dto.getDocumento().trim();
        if (!documento.equals(cobrador.getDocumento()) && cobradorRepository.existsByDocumento(documento)) {
            throw new ReglaNegocioException("Ya existe un cobrador con el documento: " + documento);
        }
        dto.aplicarA(cobrador);
        return cobradorRepository.save(cobrador);
    }

    /** Baja lógica: el cobrador conserva su historial de abonos y cuadres. */
    public void desactivar(Long id) {
        Cobrador cobrador = obtener(id);
        cobrador.setActivo(false);
        cobradorRepository.save(cobrador);
    }
}
