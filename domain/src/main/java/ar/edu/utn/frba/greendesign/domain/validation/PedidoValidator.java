package ar.edu.utn.frba.greendesign.domain.validation;

import ar.edu.utn.frba.greendesign.domain.dto.ItemPedidoRequest;
import ar.edu.utn.frba.greendesign.domain.dto.PedidoRequest;
import ar.edu.utn.frba.greendesign.domain.model.Producto;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PedidoValidator {

    private PedidoValidator() {}

    public static void validarStock(List<ItemPedidoRequest> items, List<Producto> productos) {
        Map<Long, Producto> porId = productos.stream()
                .collect(Collectors.toMap(Producto::id, Function.identity()));

        for (ItemPedidoRequest item : items) {
            Producto producto = porId.get(item.productoId());
            if (producto == null) {
                throw new IllegalArgumentException("Producto no encontrado: " + item.productoId());
            }
            if (producto.stock() < item.cantidad()) {
                throw new IllegalStateException(
                        "Stock insuficiente para producto " + producto.id()
                                + ": disponible=" + producto.stock()
                                + ", solicitado=" + item.cantidad());
            }
        }
    }

    public static void validarRequest(PedidoRequest request) {
        if (request.items().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un item");
        }
        long productosDistintos = request.items().stream()
                .map(ItemPedidoRequest::productoId)
                .distinct()
                .count();
        if (productosDistintos != request.items().size()) {
            throw new IllegalArgumentException("No se permiten items duplicados para el mismo producto");
        }
    }
}
