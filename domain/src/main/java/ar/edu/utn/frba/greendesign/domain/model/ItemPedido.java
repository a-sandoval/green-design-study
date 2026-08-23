package ar.edu.utn.frba.greendesign.domain.model;

import java.math.BigDecimal;

public record ItemPedido(
        Long id,
        Long pedidoId,
        Long productoId,
        Integer cantidad,
        BigDecimal precioUnitario
) {}
