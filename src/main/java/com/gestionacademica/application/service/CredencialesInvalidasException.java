package com.gestionacademica.application.service;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() { super("Credenciales inválidas"); }
}
