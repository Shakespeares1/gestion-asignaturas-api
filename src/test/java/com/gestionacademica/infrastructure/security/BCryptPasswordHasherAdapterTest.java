package com.gestionacademica.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.application.service.ContrasenaInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class BCryptPasswordHasherAdapterTest {
    @Test
    void guardaUnHashVerificableQueNoEsLaContrasenaOriginal() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        BCryptPasswordHasherAdapter hasher = new BCryptPasswordHasherAdapter(encoder);
        String input = "sample-only-password";

        String hash = hasher.hash(input);

        assertNotEquals(input, hash);
        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(encoder.matches(input, hash));
    }

    @Test
    void rechazaContrasenasMasLargasQueElLimiteDeBytesDeBCrypt() {
        BCryptPasswordHasherAdapter hasher = new BCryptPasswordHasherAdapter(
                PasswordEncoderFactories.createDelegatingPasswordEncoder());

        assertThrows(ContrasenaInvalidaException.class, () -> hasher.hash("á".repeat(37)));
    }
}
