package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.Asignatura;
import java.util.Optional;

public interface ObtenerAsignaturaUseCase {
    Optional<Asignatura> obtener(Long id);
}
