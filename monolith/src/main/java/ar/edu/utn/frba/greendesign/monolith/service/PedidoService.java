package ar.edu.utn.frba.greendesign.monolith.service;

import ar.edu.utn.frba.greendesign.domain.dto.ItemPedidoRequest;
import ar.edu.utn.frba.greendesign.domain.dto.PedidoDetailResponse;
import ar.edu.utn.frba.greendesign.domain.dto.PedidoRequest;
import ar.edu.utn.frba.greendesign.domain.model.ItemPedido;
import ar.edu.utn.frba.greendesign.domain.model.Pedido;
import ar.edu.utn.frba.greendesign.domain.model.Producto;
import ar.edu.utn.frba.greendesign.domain.validation.PedidoValidator;
import ar.edu.utn.frba.greendesign.monolith.repository.ClienteRepository;
import ar.edu.utn.frba.greendesign.monolith.repository.PedidoRepository;
import ar.edu.utn.frba.greendesign.monolith.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
    }

    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    public Optional<PedidoDetailResponse> obtenerDetalle(Long id) {
        return pedidoRepository.findDetailById(id);
    }

    @Transactional
    public Pedido crear(PedidoRequest request) {
        PedidoValidator.validarRequest(request);

        clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.clienteId()));

        List<Long> productoIds = request.items().stream()
                .map(ItemPedidoRequest::productoId)
                .toList();
        List<Producto> productos = productoRepository.findByIds(productoIds);
        PedidoValidator.validarStock(request.items(), productos);

        BigDecimal total = BigDecimal.ZERO;
        List<ItemPedido> items = new java.util.ArrayList<>();
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
            productoRepository.decrementStock(itemReq.productoId(), itemReq.cantidad());
        }

        return pedidoRepository.save(request.clienteId(), total, items);
    }

    @Transactional
    public boolean cancelar(Long id) {
        return pedidoRepository.updateEstado(id, "CANCELADO");
    }

    @Transactional
    public boolean eliminar(Long id) {
        return pedidoRepository.delete(id);
    }
}
