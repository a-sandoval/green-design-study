package ar.edu.utn.frba.greendesign.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Pedido(
        Long id,
        Long clienteId,
        LocalDateTime fecha,
        String estado,
        BigDecimal total,
        List<ItemPedido> items
) {}
