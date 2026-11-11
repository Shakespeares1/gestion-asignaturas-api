package com.gestionacademica.application.port.output;

import com.gestionacademica.domain.model.Usuario;
import java.time.Instant;

public interface JwtIssuerPort {
    IssuedToken issue(Usuario usuario);

    record IssuedToken(String value, Instant expiresAt) {}
}
