package ar.edu.utn.frba.greendesign.monolith.controller;

import ar.edu.utn.frba.greendesign.domain.dto.PedidoDetailResponse;
import ar.edu.utn.frba.greendesign.domain.dto.PedidoRequest;
import ar.edu.utn.frba.greendesign.domain.model.Pedido;
import ar.edu.utn.frba.greendesign.monolith.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Pedido> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDetailResponse> obtener(@PathVariable Long id) {
        return service.obtenerDetalle(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@RequestBody PedidoRequest request) {
        Pedido pedido = service.crear(request);
        return ResponseEntity.created(URI.create("/api/pedidos/" + pedido.id())).body(pedido);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        return service.cancelar(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return service.eliminar(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
