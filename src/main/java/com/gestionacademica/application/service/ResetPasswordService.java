package com.gestionacademica.application.service;

import com.gestionacademica.application.port.input.ResetPasswordUseCase;
import com.gestionacademica.application.port.output.PasswordHasherPort;
import com.gestionacademica.application.port.output.PasswordResetTokenPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import java.time.Clock;

public class ResetPasswordService implements ResetPasswordUseCase {
    private final UsuarioRepositoryPort usuarios;
    private final PasswordResetTokenPort tokens;
    private final PasswordHasherPort passwords;
    private final Clock clock;

    public ResetPasswordService(UsuarioRepositoryPort usuarios, PasswordResetTokenPort tokens,
            PasswordHasherPort passwords, Clock clock) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.passwords = passwords;
        this.clock = clock;
    }

    @Override
    public boolean reset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) return false;
        if (!PasswordPolicy.isValid(newPassword)) {
            throw new ContrasenaResetInvalidaException(PasswordPolicy.validationMessage());
        }
        String passwordHash = passwords.hash(newPassword);
        return usuarios.restablecerPasswordSiTokenVigente(tokens.hash(rawToken), clock.instant(), passwordHash);
    }
}
