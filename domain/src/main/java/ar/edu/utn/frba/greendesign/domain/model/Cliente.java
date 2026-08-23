package ar.edu.utn.frba.greendesign.domain.model;

public record Cliente(
        Long id,
        String nombre,
        String email,
        String direccion
) {}
