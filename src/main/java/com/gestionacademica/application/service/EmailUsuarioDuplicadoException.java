package com.gestionacademica.application.service;

public class EmailUsuarioDuplicadoException extends RuntimeException {
    public EmailUsuarioDuplicadoException() {
        super("Ya existe un usuario con ese correo electrónico");
    }
}
