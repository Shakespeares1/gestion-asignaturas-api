package com.gestionacademica.application.service;

import com.gestionacademica.application.port.input.LoginUseCase;
import com.gestionacademica.application.port.output.JwtIssuerPort;
import com.gestionacademica.application.port.output.PasswordVerifierPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.Usuario;

public class LoginService implements LoginUseCase {
    private final UsuarioRepositoryPort usuarios;
    private final PasswordVerifierPort passwords;
    private final JwtIssuerPort jwtIssuer;

    public LoginService(UsuarioRepositoryPort usuarios, PasswordVerifierPort passwords, JwtIssuerPort jwtIssuer) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.jwtIssuer = jwtIssuer;
    }

    @Override
    public LoginResult login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new CredencialesInvalidasException();
        }
        final String normalized;
        try { normalized = Usuario.normalizarEmail(email); }
        catch (IllegalArgumentException exception) { throw new CredencialesInvalidasException(); }

        Usuario user = usuarios.buscarPorEmail(normalized).orElseThrow(CredencialesInvalidasException::new);
        if (user.getAccountStatus() != EstadoCuenta.ACTIVO) throw new CuentaInactivaException();
        if (!passwords.matches(password, user.getPasswordHash())) throw new CredencialesInvalidasException();
        JwtIssuerPort.IssuedToken token = jwtIssuer.issue(user);
        return new LoginResult(token, user.getId(), user.getNombreCompleto(), user.getEmail(), user.getRole().name());
    }
}
