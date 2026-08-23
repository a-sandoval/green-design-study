package ar.edu.utn.frba.greendesign.clientes.service;

import ar.edu.utn.frba.greendesign.domain.dto.ClienteRequest;
import ar.edu.utn.frba.greendesign.domain.model.Cliente;
import ar.edu.utn.frba.greendesign.clientes.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listar() {
        return repository.findAll();
    }

    public Optional<Cliente> obtener(Long id) {
        return repository.findById(id);
    }

    public Cliente crear(ClienteRequest request) {
        return repository.save(request.nombre(), request.email(), request.direccion());
    }

    public Optional<Cliente> actualizar(Long id, ClienteRequest request) {
        return repository.update(id, request.nombre(), request.email(), request.direccion());
    }

    public boolean eliminar(Long id) {
        return repository.delete(id);
    }
}
