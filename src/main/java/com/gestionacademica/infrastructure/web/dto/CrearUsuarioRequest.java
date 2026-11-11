package com.gestionacademica.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gestionacademica.infrastructure.web.validation.ValidPassword;

public class CrearUsuarioRequest {
    @NotBlank
    @Schema(description = "Nombre público del usuario", example = "Ada Lovelace")
    private String nombreCompleto;

    @NotBlank
    @Email
    @Size(max = 254)
    @Schema(description = "Email único; se normaliza al guardar", example = "ada@example.org")
    private String email;

    @NotBlank
    @Size(min = 12, max = 72)
    @ValidPassword
    @Schema(description = "Contraseña de al menos 12 caracteres; BCrypt admite hasta 72 bytes UTF-8. Se almacena solo su hash.", example = "************")
    private String password;

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
