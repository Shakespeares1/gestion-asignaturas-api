package com.gestionacademica.infrastructure.security;

import com.gestionacademica.application.port.output.PasswordHasherPort;
import com.gestionacademica.application.port.output.PasswordVerifierPort;
import com.gestionacademica.application.service.ContrasenaInvalidaException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BCryptPasswordHasherAdapter implements PasswordHasherPort, PasswordVerifierPort {
    private static final int BCRYPT_MAX_INPUT_BYTES = 72;
    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasherAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new ContrasenaInvalidaException("La contraseña es obligatoria");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_INPUT_BYTES) {
            throw new ContrasenaInvalidaException("La contraseña excede el límite de bytes de BCrypt");
        }
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || rawPassword.isBlank() || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_INPUT_BYTES) return false;
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
