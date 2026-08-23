package ar.edu.utn.frba.greendesign.productos.service;

import ar.edu.utn.frba.greendesign.domain.dto.ProductoRequest;
import ar.edu.utn.frba.greendesign.domain.model.Producto;
import ar.edu.utn.frba.greendesign.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<Producto> listar() {
        return repository.findAll();
    }

    public Optional<Producto> obtener(Long id) {
        return repository.findById(id);
    }

    public List<Producto> obtenerPorIds(List<Long> ids) {
        return repository.findByIds(ids);
    }

    public Producto crear(ProductoRequest request) {
        return repository.save(request.nombre(), request.descripcion(), request.precio(), request.stock());
    }

    public Optional<Producto> actualizar(Long id, ProductoRequest request) {
        return repository.update(id, request.nombre(), request.descripcion(), request.precio(), request.stock());
    }

    public void decrementarStock(Long productoId, int cantidad) {
        repository.decrementStock(productoId, cantidad);
    }

    public boolean eliminar(Long id) {
        return repository.delete(id);
    }
}
