
package com.gestionacademica.application.port.output;

import com.gestionacademica.domain.model.Asignatura;
import java.util.List;
import java.util.Optional;

public interface AsignaturaRepositoryPort {

    Asignatura guardar(Asignatura asignatura);

    List<Asignatura> listar();

    Optional<Asignatura> buscarPorId(Long id);

    boolean existePorId(Long id);

    void eliminarPorId(Long id);

}
