package com.gestionacademica.infrastructure.web.dto;

import com.gestionacademica.infrastructure.web.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CrearUsuarioRequest {
    @NotBlank
    @Size(min = 3, max = 30)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{1,28}[A-Za-z0-9]$")
    @Schema(description = "Username único, normalizado sin distinguir mayúsculas; 3-30 letras, números, punto, guion o guion bajo",
            example = "ada.lovelace")
    private String username;

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
    @Schema(description = "Contraseña de al menos 12 caracteres; BCrypt admite hasta 72 bytes UTF-8. Se almacena solo su hash.",
            example = "************")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
