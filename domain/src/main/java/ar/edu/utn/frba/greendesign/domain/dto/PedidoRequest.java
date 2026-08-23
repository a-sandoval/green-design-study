package ar.edu.utn.frba.greendesign.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PedidoRequest(
        @NotNull Long clienteId,
        @NotEmpty List<ItemPedidoRequest> items
) {}
