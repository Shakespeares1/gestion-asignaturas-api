package com.gestionacademica.application.port.output;

import java.time.Instant;

public interface PasswordResetEmailPort {
    boolean isAvailable();
    void sendResetLink(String recipient, String rawToken, Instant expiresAt);
}
