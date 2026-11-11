package com.gestionacademica.infrastructure.web.dto;

import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record ActualizarAccesoUsuarioRequest(
        @Schema(description = "ADMIN o USER", example = "USER") @NotNull RolUsuario role,
        @Schema(description = "ACTIVO o INACTIVO", example = "ACTIVO") @NotNull EstadoCuenta accountStatus) {}
