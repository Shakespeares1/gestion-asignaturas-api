package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.Asignatura;

public interface ActualizarAsignaturaUseCase {
    Asignatura actualizar(Long id, Asignatura asignatura);
}
