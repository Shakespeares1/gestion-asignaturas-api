
package com.gestionacademica.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.gestionacademica.application.port.input.CrearAsignaturaUseCase;
import com.gestionacademica.application.port.output.AsignaturaRepositoryPort;
import com.gestionacademica.domain.model.Asignatura;

@Service
public class CrearAsignaturaService implements CrearAsignaturaUseCase {

    private final AsignaturaRepositoryPort asignaturaRepositoryPort;

    public CrearAsignaturaService(
            AsignaturaRepositoryPort asignaturaRepositoryPort) {

        this.asignaturaRepositoryPort = asignaturaRepositoryPort;
    }

    @Override
    public Asignatura ejecutar(Asignatura asignatura) {

        Objects.requireNonNull(
                asignatura,
                "La asignatura no puede ser nula"
        );

        return asignaturaRepositoryPort.guardar(asignatura);
    }
}
