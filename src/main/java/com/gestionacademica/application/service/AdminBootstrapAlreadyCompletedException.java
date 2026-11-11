package com.gestionacademica.application.service;

public class AdminBootstrapAlreadyCompletedException extends RuntimeException {
    public AdminBootstrapAlreadyCompletedException() {
        super("Ya existe un administrador; el bootstrap inicial no se puede reutilizar");
    }
}
