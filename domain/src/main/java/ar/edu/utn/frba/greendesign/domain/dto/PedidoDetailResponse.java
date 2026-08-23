package ar.edu.utn.frba.greendesign.domain.dto;

import ar.edu.utn.frba.greendesign.domain.model.Cliente;
import ar.edu.utn.frba.greendesign.domain.model.Pedido;
import ar.edu.utn.frba.greendesign.domain.model.Producto;

import java.util.List;

public record PedidoDetailResponse(
        Pedido pedido,
        Cliente cliente,
        List<ProductoConCantidad> productos
) {
    public record ProductoConCantidad(Producto producto, Integer cantidad) {}
}
