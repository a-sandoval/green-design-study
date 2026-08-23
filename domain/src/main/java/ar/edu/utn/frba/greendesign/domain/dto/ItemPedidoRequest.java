package ar.edu.utn.frba.greendesign.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedidoRequest(
        @NotNull Long productoId,
        @NotNull @Positive Integer cantidad
) {}
