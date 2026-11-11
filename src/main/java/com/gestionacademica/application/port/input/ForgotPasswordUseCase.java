package com.gestionacademica.application.port.input;

public interface ForgotPasswordUseCase {
    void requestReset(String email);
}
