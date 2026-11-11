package com.gestionacademica.application.service;

public class CorreoNoDisponibleException extends RuntimeException {
    public CorreoNoDisponibleException() { super("La entrega de correo no está disponible"); }
}
