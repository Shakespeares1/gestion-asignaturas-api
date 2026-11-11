package com.gestionacademica.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class LoginRequest {
    @NotBlank @Email @Size(max = 254)
    @Schema(example = "ada@example.org")
    private String email;
    @NotBlank @Size(max = 72)
    @Schema(description = "Contraseña sin hash", example = "********")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
