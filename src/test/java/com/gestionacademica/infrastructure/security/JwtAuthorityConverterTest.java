package com.gestionacademica.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gestionacademica.infrastructure.config.SecurityConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAuthorityConverterTest {
    @Test
    void convierteRolesDelTokenEnAuthoritiesSpring() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
                .subject("7").claim("roles", List.of("ADMIN", "USER"))
                .issuedAt(now).expiresAt(now.plusSeconds(60)).build();

        var authentication = new SecurityConfiguration().jwtAuthenticationConverter().convert(jwt);

        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
