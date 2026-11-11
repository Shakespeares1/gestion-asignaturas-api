package com.gestionacademica.application.service;

import com.gestionacademica.application.port.input.ActualizarAsignaturaUseCase;
import com.gestionacademica.application.port.input.EliminarAsignaturaUseCase;
import com.gestionacademica.application.port.input.ListarAsignaturasUseCase;
import com.gestionacademica.application.port.input.ObtenerAsignaturaUseCase;
import com.gestionacademica.application.port.output.AsignaturaRepositoryPort;
import com.gestionacademica.domain.model.Asignatura;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AsignaturaCrudService implements ListarAsignaturasUseCase, ObtenerAsignaturaUseCase,
        ActualizarAsignaturaUseCase, EliminarAsignaturaUseCase {
    private final AsignaturaRepositoryPort repository;

    public AsignaturaCrudService(AsignaturaRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override public List<Asignatura> listar() { return repository.listar(); }
    @Override public Optional<Asignatura> obtener(Long id) { return repository.buscarPorId(id); }

    @Override
    public Asignatura actualizar(Long id, Asignatura asignatura) {
        Objects.requireNonNull(asignatura, "La asignatura no puede ser nula");
        if (!repository.existePorId(id)) throw new AsignaturaNoEncontradaException(id);
        return repository.guardar(new Asignatura(id, asignatura.getNombre(), asignatura.getNombreCompleto(),
                asignatura.getDescripcion(), asignatura.getAreaConocimiento(), asignatura.getCarrera(),
                asignatura.getNumeroCreditos(), asignatura.getContenidoTematico(), asignatura.getSemestre(),
                asignatura.getProfesor()));
    }

    @Override
    public boolean eliminar(Long id) {
        if (!repository.existePorId(id)) return false;
        repository.eliminarPorId(id);
        return true;
    }
}
