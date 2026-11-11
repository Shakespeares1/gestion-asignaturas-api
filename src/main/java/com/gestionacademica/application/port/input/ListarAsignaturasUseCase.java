package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.Asignatura;
import java.util.List;

public interface ListarAsignaturasUseCase {
    List<Asignatura> listar();
}
