package com.gestionacademica.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtConfigurationTest {
    private final JwtConfiguration configuration = new JwtConfiguration();

    @Test
    void noProporcionaClavePredeterminada() {
        assertThrows(IllegalStateException.class, () -> configuration.jwtEncoder(""));
        assertThrows(IllegalStateException.class, () -> configuration.jwtDecoder("", "issuer"));
    }

    @Test
    void rechazaClaveDeMenosDe256Bits() {
        String shortKey = java.util.Base64.getEncoder().encodeToString(new byte[31]);
        assertThrows(IllegalStateException.class, () -> configuration.jwtEncoder(shortKey));
    }
}
