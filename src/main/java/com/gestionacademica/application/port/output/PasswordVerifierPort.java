package com.gestionacademica.application.port.output;

public interface PasswordVerifierPort {
    boolean matches(String rawPassword, String passwordHash);
}
