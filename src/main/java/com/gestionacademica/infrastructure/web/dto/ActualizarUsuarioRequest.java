package com.gestionacademica.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record ActualizarUsuarioRequest(
        @Schema(example = "Ada Lovelace") @NotBlank String nombreCompleto,
        @Schema(example = "ada@example.org") @NotBlank @Email @Size(max = 254) String email) {
}
