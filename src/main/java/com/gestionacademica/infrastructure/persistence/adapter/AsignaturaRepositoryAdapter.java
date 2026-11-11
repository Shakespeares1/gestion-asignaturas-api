package com.gestionacademica.infrastructure.persistence.adapter;

import com.gestionacademica.application.port.output.AsignaturaRepositoryPort;
import com.gestionacademica.domain.model.Asignatura;
import com.gestionacademica.infrastructure.persistence.mapper.AsignaturaMapper;
import com.gestionacademica.infrastructure.persistence.repository.SpringDataAsignaturaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AsignaturaRepositoryAdapter implements AsignaturaRepositoryPort {
    private final SpringDataAsignaturaRepository repository;
    public AsignaturaRepositoryAdapter(SpringDataAsignaturaRepository repository) { this.repository = repository; }
    @Override public Asignatura guardar(Asignatura a) { return AsignaturaMapper.toDomain(repository.save(AsignaturaMapper.toEntity(a))); }
    @Override public List<Asignatura> listar() { return repository.findAll().stream().map(AsignaturaMapper::toDomain).toList(); }
    @Override public Optional<Asignatura> buscarPorId(Long id) { return repository.findById(id).map(AsignaturaMapper::toDomain); }
    @Override public boolean existePorId(Long id) { return repository.existsById(id); }
    @Override public void eliminarPorId(Long id) { repository.deleteById(id); }
}
