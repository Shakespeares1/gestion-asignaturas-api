package com.gestionacademica.application.service;

import com.gestionacademica.application.port.input.ForgotPasswordUseCase;
import com.gestionacademica.application.port.output.PasswordResetEmailPort;
import com.gestionacademica.application.port.output.PasswordResetTokenPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.Usuario;
import com.gestionacademica.domain.model.EstadoCuenta;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForgotPasswordService implements ForgotPasswordUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPasswordService.class);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private final UsuarioRepositoryPort usuarios;
    private final PasswordResetTokenPort tokens;
    private final PasswordResetEmailPort email;
    private final Clock clock;

    public ForgotPasswordService(UsuarioRepositoryPort usuarios, PasswordResetTokenPort tokens,
            PasswordResetEmailPort email, Clock clock) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.email = email;
        this.clock = clock;
    }

    @Override
    public void requestReset(String emailAddress) {
        if (!email.isAvailable()) throw new CorreoNoDisponibleException();
        final String normalized;
        try { normalized = Usuario.normalizarEmail(emailAddress); }
        catch (IllegalArgumentException exception) { return; }
        usuarios.buscarPorEmail(normalized).filter(user -> user.getAccountStatus() == EstadoCuenta.ACTIVO)
                .ifPresent(user -> enviar(user));
    }

    private void enviar(Usuario user) {
        PasswordResetTokenPort.GeneratedToken token = tokens.generate();
        var expiresAt = clock.instant().plus(TOKEN_TTL);
        usuarios.guardar(user.conTokenRecuperacion(token.tokenHash(), expiresAt));
        try {
            email.sendResetLink(user.getEmail(), token.rawToken(), expiresAt);
        } catch (RuntimeException exception) {
            usuarios.guardar(user.limpiarTokenRecuperacion());
            LOGGER.warn("Password reset email delivery failed; reset token invalidated");
        }
    }
}
