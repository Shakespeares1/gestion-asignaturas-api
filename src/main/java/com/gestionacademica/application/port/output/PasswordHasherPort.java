package com.gestionacademica.application.port.output;

public interface PasswordHasherPort {
    String hash(String password);
}
