package com.gestionacademica.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class ForgotPasswordRequest {
    @NotBlank @Email @Size(max = 254)
    @Schema(example = "ada@example.org")
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
