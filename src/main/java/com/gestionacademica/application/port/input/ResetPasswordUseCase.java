package com.gestionacademica.application.port.input;

public interface ResetPasswordUseCase {
    boolean reset(String rawToken, String newPassword);
}
