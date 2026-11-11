package com.gestionacademica.application.service;

public class ContrasenaInvalidaException extends RuntimeException {
    public ContrasenaInvalidaException(String message) {
        super(message);
    }
}
