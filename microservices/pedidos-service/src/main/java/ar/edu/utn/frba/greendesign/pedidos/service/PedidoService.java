package ar.edu.utn.frba.greendesign.pedidos.service;

import ar.edu.utn.frba.greendesign.domain.dto.ItemPedidoRequest;
import ar.edu.utn.frba.greendesign.domain.dto.PedidoDetailResponse;
import ar.edu.utn.frba.greendesign.domain.dto.PedidoRequest;
import ar.edu.utn.frba.greendesign.domain.model.Cliente;
import ar.edu.utn.frba.greendesign.domain.model.ItemPedido;
import ar.edu.utn.frba.greendesign.domain.model.Pedido;
import ar.edu.utn.frba.greendesign.domain.model.Producto;
import ar.edu.utn.frba.greendesign.domain.validation.PedidoValidator;
import ar.edu.utn.frba.greendesign.pedidos.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final WebClient clientesClient;
    private final WebClient productosClient;

    public PedidoService(PedidoRepository repository,
                         @Qualifier("clientesWebClient") WebClient clientesClient,
                         @Qualifier("productosWebClient") WebClient productosClient) {
        this.repository = repository;
        this.clientesClient = clientesClient;
        this.productosClient = productosClient;
    }

    public List<Pedido> listar() {
        return repository.findAll();
    }

    /**
     * Operación clave del experimento: obtener pedido con cliente y productos por HTTP.
     * En el monolito esto es 1 query con JOIN; acá son 2 llamadas HTTP + 1 query local.
     * Fan-out por lotes (no N+1): 1 call a clientes/{id}, 1 call a productos/batch?ids=...
     */
    public Optional<PedidoDetailResponse> obtenerDetalle(Long id) {
        Optional<Pedido> maybePedido = repository.findById(id);
        if (maybePedido.isEmpty()) return Optional.empty();

        Pedido pedido = maybePedido.get();
        List<Long> productoIds = pedido.items().stream()
                .map(ItemPedido::productoId)
                .toList();

        String idsParam = productoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Mono<Cliente> clienteMono = clientesClient.get()
                .uri("/api/clientes/{id}", pedido.clienteId())
                .retrieve()
                .bodyToMono(Cliente.class);

        Mono<List<Producto>> productosMono = productosClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/productos/batch")
                        .queryParam("ids", idsParam)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});

        // Fan-out concurrente: ambas llamadas HTTP en paralelo
        var tuple = Mono.zip(clienteMono, productosMono).block();
        Cliente cliente = tuple.getT1();
        List<Producto> productos = tuple.getT2();

        List<PedidoDetailResponse.ProductoConCantidad> productosConCantidad = pedido.items().stream()
                .map(item -> {
                    Producto producto = productos.stream()
                            .filter(p -> p.id().equals(item.productoId()))
                            .findFirst()
                            .orElseThrow();
                    return new PedidoDetailResponse.ProductoConCantidad(producto, item.cantidad());
                })
                .toList();

        return Optional.of(new PedidoDetailResponse(pedido, cliente, productosConCantidad));
    }

    @Transactional
    public Pedido crear(PedidoRequest request) {
        PedidoValidator.validarRequest(request);

        Cliente cliente = clientesClient.get()
                .uri("/api/clientes/{id}", request.clienteId())
                .retrieve()
                .bodyToMono(Cliente.class)
                .block();
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente no encontrado: " + request.clienteId());
        }

        List<Long> productoIds = request.items().stream()
                .map(ItemPedidoRequest::productoId)
                .toList();
        String idsParam = productoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        List<Producto> productos = productosClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/productos/batch")
                        .queryParam("ids", idsParam)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Producto>>() {})
                .block();

        PedidoValidator.validarStock(request.items(), productos);

        BigDecimal total = BigDecimal.ZERO;
        List<ItemPedido> items = new ArrayList<>();
        for (ItemPedidoRequest itemReq : request.items()) {
            Producto producto = productos.stream()
                    .filter(p -> p.id().equals(itemReq.productoId()))
                    .findFirst()
                    .orElseThrow();
            BigDecimal subtotal = producto.precio().multiply(BigDecimal.valueOf(itemReq.cantidad()));
            total = total.add(subtotal);
            items.add(new ItemPedido(null, null, producto.id(), itemReq.cantidad(), producto.precio()));
        }

        for (ItemPedidoRequest itemReq : request.items()) {
            productosClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/productos/{id}/decrementar-stock")
                            .queryParam("cantidad", itemReq.cantidad())
                            .build(itemReq.productoId()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        }

        return repository.save(request.clienteId(), total, items);
    }

    @Transactional
    public boolean cancelar(Long id) {
        return repository.updateEstado(id, "CANCELADO");
    }

    @Transactional
    public boolean eliminar(Long id) {
        return repository.delete(id);
    }
}
