package com.gestionacademica.application.service;

public class UsernameUsuarioDuplicadoException extends RuntimeException {
    public UsernameUsuarioDuplicadoException() {
        super("Ya existe un usuario con ese username");
    }
}
