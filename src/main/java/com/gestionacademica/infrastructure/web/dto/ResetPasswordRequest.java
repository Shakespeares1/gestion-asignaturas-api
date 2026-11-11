package com.gestionacademica.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gestionacademica.infrastructure.web.validation.ValidPassword;

public class ResetPasswordRequest {
    @NotBlank @Size(max = 128)
    @Schema(description = "Token de un solo uso recibido por correo", example = "token-recibido-por-correo")
    private String token;
    @NotBlank @Size(min = 12, max = 72)
    @ValidPassword
    @Schema(description = "Nueva contraseña de al menos 12 caracteres y máximo 72 bytes UTF-8", example = "************")
    private String newPassword;
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
