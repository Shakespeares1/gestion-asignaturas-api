package com.gestionacademica.infrastructure.security;

import com.gestionacademica.application.port.output.JwtIssuerPort;
import com.gestionacademica.domain.model.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

public class NimbusJwtIssuerAdapter implements JwtIssuerPort {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration ttl;
    private final String issuer;

    public NimbusJwtIssuerAdapter(JwtEncoder encoder, Clock clock, Duration ttl, String issuer) {
        this.encoder = encoder;
        this.clock = clock;
        this.ttl = ttl;
        this.issuer = issuer;
    }

    @Override
    public IssuedToken issue(Usuario usuario) {
        Instant now = clock.instant();
        Instant expiry = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(expiry)
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("name", usuario.getNombreCompleto())
                .claim("roles", List.of(usuario.getRole().name()))
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(value, expiry);
    }
}
