package ar.edu.utn.frba.greendesign.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
        @NotBlank String nombre,
        @Email @NotBlank String email,
        @NotBlank String direccion
) {}
