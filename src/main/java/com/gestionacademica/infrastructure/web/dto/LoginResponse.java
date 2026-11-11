package com.gestionacademica.infrastructure.web.dto;

import com.gestionacademica.application.port.input.LoginUseCase.LoginResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acceso y perfil público; no incluye hash o contraseña")
public record LoginResponse(String accessToken, String tokenType, long expiresIn, PublicUser user) {
    public static LoginResponse from(LoginResult result) {
        long seconds = Math.max(0, result.token().expiresAt().getEpochSecond() - java.time.Instant.now().getEpochSecond());
        return new LoginResponse(result.token().value(), "Bearer", seconds,
                new PublicUser(result.userId(), result.nombreCompleto(), result.email(), result.role()));
    }

    public record PublicUser(Long id, String nombreCompleto, String email, String role) {}
}
