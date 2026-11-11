package com.gestionacademica.application.port.input;

import com.gestionacademica.application.port.output.JwtIssuerPort.IssuedToken;

public interface LoginUseCase {
    LoginResult login(String email, String password);

    record LoginResult(IssuedToken token, Long userId, String nombreCompleto, String email, String role) {}
}
