package com.gestionacademica.application.service;

public class AsignaturaNoEncontradaException extends RuntimeException {
    public AsignaturaNoEncontradaException(Long id) { super("No existe la asignatura con id " + id); }
}
