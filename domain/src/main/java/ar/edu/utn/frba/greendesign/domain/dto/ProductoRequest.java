package ar.edu.utn.frba.greendesign.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @Positive BigDecimal precio,
        @NotNull @PositiveOrZero Integer stock
) {}
