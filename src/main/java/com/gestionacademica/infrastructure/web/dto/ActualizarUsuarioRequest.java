package com.gestionacademica.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @Schema(example = "Ada Lovelace") @NotBlank String nombreCompleto,
        @Schema(example = "ada@example.org") @NotBlank @Email @Size(max = 254) String email,
        @Schema(description = "Opcional para compatibilidad con clientes anteriores; si se envía debe ser único",
                example = "ada.lovelace")
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{1,28}[A-Za-z0-9]$")
        String username) {
}
