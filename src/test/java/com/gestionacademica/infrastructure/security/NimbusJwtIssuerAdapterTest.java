package com.gestionacademica.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class NimbusJwtIssuerAdapterTest {
    private static final Instant NOW = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    private SecretKey key;
    private NimbusJwtIssuerAdapter issuer;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("HmacSHA256");
        generator.init(256);
        key = generator.generateKey();
        issuer = new NimbusJwtIssuerAdapter(new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(60), "test-issuer");
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("test-issuer"));
    }

    @Test
    void emiteJwtFirmadoConUsuarioRolYExpiracion() {
        var token = issuer.issue(user());
        Jwt decoded = decoder.decode(token.value());

        assertEquals("42", decoded.getSubject());
        assertEquals("user@example.org", decoded.getClaimAsString("email"));
        assertEquals(java.util.List.of("ADMIN"), decoded.getClaimAsStringList("roles"));
        assertEquals(NOW.plusSeconds(60), token.expiresAt());
    }

    @Test
    void decoderRechazaJwtExpirado() {
        NimbusJwtIssuerAdapter expiredIssuer = new NimbusJwtIssuerAdapter(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                Clock.fixed(NOW.minusSeconds(120), ZoneOffset.UTC), Duration.ofSeconds(60), "test-issuer");
        assertThrows(JwtException.class, () -> decoder.decode(expiredIssuer.issue(user()).value()));
    }

    @Test
    void decoderRechazaJwtManipulado() {
        String token = issuer.issue(user()).value();
        String[] parts = token.split("\\.");
        char replacement = parts[2].charAt(0) == 'A' ? 'B' : 'A';
        parts[2] = replacement + parts[2].substring(1);
        assertThrows(JwtException.class, () -> decoder.decode(String.join(".", parts)));
    }

    private static Usuario user() {
        return new Usuario(42L, "Test User", "user@example.org", "{bcrypt}hash", RolUsuario.ADMIN,
                EstadoCuenta.ACTIVO, NOW, null, null);
    }
}
