package ar.edu.utn.frba.greendesign.domain.model;

import java.math.BigDecimal;

public record Producto(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock
) {}
