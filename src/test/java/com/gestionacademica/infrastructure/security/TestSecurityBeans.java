package com.gestionacademica.infrastructure.security;

import com.gestionacademica.application.port.output.JwtIssuerPort;
import com.gestionacademica.application.port.output.PasswordResetEmailPort;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class TestSecurityBeans {
    @Bean
    JwtIssuerPort testJwtIssuerPort() {
        return usuario -> new JwtIssuerPort.IssuedToken("test-token", Instant.now().plusSeconds(60));
    }

    @Bean
    JwtDecoder testJwtDecoder() {
        return token -> { throw new UnsupportedOperationException("El decoder no se usa en estas pruebas"); };
    }

    @Bean
    PasswordResetEmailPort testPasswordResetEmailPort() {
        return new PasswordResetEmailPort() {
            @Override public boolean isAvailable() { return false; }
            @Override public void sendResetLink(String recipient, String rawToken, Instant expiresAt) {
                throw new UnsupportedOperationException("No se envía correo durante las pruebas");
            }
        };
    }
}
