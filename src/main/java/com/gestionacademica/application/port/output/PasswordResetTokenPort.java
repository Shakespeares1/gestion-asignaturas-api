package com.gestionacademica.application.port.output;

public interface PasswordResetTokenPort {
    GeneratedToken generate();
    String hash(String rawToken);

    record GeneratedToken(String rawToken, String tokenHash) {}
}
