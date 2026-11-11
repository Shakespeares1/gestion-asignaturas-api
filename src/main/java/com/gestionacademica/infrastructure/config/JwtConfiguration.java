package com.gestionacademica.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@Profile("!test")
public class JwtConfiguration {
    @Bean
    @ConditionalOnMissingBean(JwtEncoder.class)
    JwtEncoder jwtEncoder(@Value("${JWT_SECRET_BASE64:}") String encodedSecret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(encodedSecret)));
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(@Value("${JWT_SECRET_BASE64:}") String encodedSecret,
            @Value("${JWT_ISSUER:gestion-academica}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key(encodedSecret))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private SecretKey key(String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("Debe configurarse JWT_SECRET_BASE64 en el entorno");
        }
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 debe ser Base64 válido", exception);
        }
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET_BASE64 debe contener al menos 32 bytes aleatorios");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
