package com.gestionacademica.application.service;

public class CuentaInactivaException extends RuntimeException {
    public CuentaInactivaException() { super("La cuenta no está activa"); }
}
