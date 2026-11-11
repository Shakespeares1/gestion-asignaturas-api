package com.gestionacademica.infrastructure.config;

import com.gestionacademica.application.port.output.PasswordHasherPort;
import com.gestionacademica.application.port.output.PasswordVerifierPort;
import com.gestionacademica.application.port.input.ForgotPasswordUseCase;
import com.gestionacademica.application.port.input.CambiarAccesoUsuarioUseCase;
import com.gestionacademica.application.port.input.LoginUseCase;
import com.gestionacademica.application.port.input.ResetPasswordUseCase;
import com.gestionacademica.application.port.output.JwtIssuerPort;
import com.gestionacademica.application.port.output.PasswordResetEmailPort;
import com.gestionacademica.application.port.output.PasswordResetTokenPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.application.service.UsuarioCrudService;
import com.gestionacademica.application.service.LoginService;
import com.gestionacademica.application.service.CambiarAccesoUsuarioService;
import com.gestionacademica.application.service.ForgotPasswordService;
import com.gestionacademica.application.service.ResetPasswordService;
import com.gestionacademica.infrastructure.security.BCryptPasswordHasherAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UsuarioConfiguration {
    @Bean PasswordEncoder usuarioPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean BCryptPasswordHasherAdapter bcryptPasswordAdapter(PasswordEncoder usuarioPasswordEncoder) {
        return new BCryptPasswordHasherAdapter(usuarioPasswordEncoder);
    }

    @Bean Clock usuarioClock() { return Clock.systemUTC(); }

    @Bean UsuarioCrudService usuarioCrudService(UsuarioRepositoryPort repository,
            PasswordHasherPort passwordHasher, Clock usuarioClock) {
        return new UsuarioCrudService(repository, passwordHasher, usuarioClock);
    }

    @Bean CambiarAccesoUsuarioUseCase cambiarAccesoUsuarioUseCase(UsuarioRepositoryPort repository) {
        return new CambiarAccesoUsuarioService(repository);
    }

    @Bean LoginUseCase loginUseCase(UsuarioRepositoryPort repository, PasswordVerifierPort verifier,
            JwtIssuerPort jwtIssuer) {
        return new LoginService(repository, verifier, jwtIssuer);
    }

    @Bean ForgotPasswordUseCase forgotPasswordUseCase(UsuarioRepositoryPort repository,
            PasswordResetTokenPort tokens, PasswordResetEmailPort email, Clock usuarioClock) {
        return new ForgotPasswordService(repository, tokens, email, usuarioClock);
    }

    @Bean ResetPasswordUseCase resetPasswordUseCase(UsuarioRepositoryPort repository,
            PasswordResetTokenPort tokens, PasswordHasherPort passwordHasher, Clock usuarioClock) {
        return new ResetPasswordService(repository, tokens, passwordHasher, usuarioClock);
    }

    @Bean
    @ConditionalOnMissingBean(JwtIssuerPort.class)
    JwtIssuerPort jwtIssuerPort(org.springframework.security.oauth2.jwt.JwtEncoder encoder,
            Clock usuarioClock, @Value("${JWT_TTL_SECONDS:900}") long ttlSeconds,
            @Value("${JWT_ISSUER:gestion-academica}") String issuer) {
        if (ttlSeconds < 60 || ttlSeconds > 86_400) throw new IllegalStateException("JWT_TTL_SECONDS debe estar entre 60 y 86400");
        return new com.gestionacademica.infrastructure.security.NimbusJwtIssuerAdapter(
                encoder, usuarioClock, Duration.ofSeconds(ttlSeconds), issuer);
    }
}
