package com.example.cobro.service;

import com.example.cobro.exception.RecursoNoEncontradoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.Cliente;
import com.example.cobro.model.Cobrador;
import com.example.cobro.repository.ClienteRepository;
import com.example.cobro.repository.CobradorRepository;
import com.example.cobro.repository.PrestamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClienteService {

    @Autowired
    private BusquedaClientesService busqueda;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private CobradorRepository cobradorRepository;

    public Cliente crearCliente(Cliente cliente) {
        return crearCliente(cliente, null);
    }

    public Cliente crearCliente(Cliente cliente, Long cobradorId) {
        cliente.setCobrador(resolverCobrador(cobradorId));
        if (clienteRepository.existsByCedula(cliente.getCedula())) {
            throw new ReglaNegocioException("Ya existe un cliente con la cédula: " + cliente.getCedula());
        }
        Cliente guardado = clienteRepository.save(cliente);
        busqueda.invalidar();
        return guardado;
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> obtenerCliente(Long id) {
        return clienteRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Cliente obtenerClienteOFallar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", id));
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> obtenerClientePorCedula(String cedula) {
        return clienteRepository.findByCedula(cedula);
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarPorCobrador(Long cobradorId) {
        return clienteRepository.findByCobrador_IdOrderByOrdenRutaAscNombreAsc(cobradorId);
    }

    private Cobrador resolverCobrador(Long cobradorId) {
        if (cobradorId == null) return null;
        Cobrador cobrador = cobradorRepository.findById(cobradorId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cobrador", cobradorId));
        if (!cobrador.isActivo()) {
            throw new ReglaNegocioException("El cobrador " + cobrador.getNombre() + " está inactivo");
        }
        return cobrador;
    }

    public Cliente actualizarCliente(Long id, Cliente datos) {
        return actualizarCliente(id, datos, null);
    }

    public Cliente actualizarCliente(Long id, Cliente datos, Long cobradorId) {
        Cliente cliente = obtenerClienteOFallar(id);

        if (!cliente.getCedula().equals(datos.getCedula()) &&
            clienteRepository.existsByCedula(datos.getCedula())) {
            throw new ReglaNegocioException("Ya existe un cliente con la cédula: " + datos.getCedula());
        }

        cliente.setCedula(datos.getCedula());
        cliente.setNombre(datos.getNombre());
        cliente.setDireccion(datos.getDireccion());
        cliente.setTelefono(datos.getTelefono());
        cliente.setOrdenRuta(datos.getOrdenRuta());
        cliente.setCobrador(resolverCobrador(cobradorId));

        Cliente guardado = clienteRepository.save(cliente);
        busqueda.invalidar();
        return guardado;
    }

    public void eliminarCliente(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw RecursoNoEncontradoException.de("Cliente", id);
        }
        if (!prestamoRepository.findByCliente_Id(id).isEmpty()) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el cliente porque tiene préstamos registrados");
        }
        clienteRepository.deleteById(id);
        busqueda.invalidar();
    }
}
