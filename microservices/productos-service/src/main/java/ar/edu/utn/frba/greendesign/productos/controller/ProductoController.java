package ar.edu.utn.frba.greendesign.productos.controller;

import ar.edu.utn.frba.greendesign.domain.dto.ProductoRequest;
import ar.edu.utn.frba.greendesign.domain.model.Producto;
import ar.edu.utn.frba.greendesign.productos.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Producto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        return service.obtener(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/batch")
    public List<Producto> obtenerPorIds(@RequestParam List<Long> ids) {
        return service.obtenerPorIds(ids);
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody ProductoRequest request) {
        Producto producto = service.crear(request);
        return ResponseEntity.created(URI.create("/api/productos/" + producto.id())).body(producto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @RequestBody ProductoRequest request) {
        return service.actualizar(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/decrementar-stock")
    public ResponseEntity<Void> decrementarStock(@PathVariable Long id, @RequestParam int cantidad) {
        service.decrementarStock(id, cantidad);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return service.eliminar(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
