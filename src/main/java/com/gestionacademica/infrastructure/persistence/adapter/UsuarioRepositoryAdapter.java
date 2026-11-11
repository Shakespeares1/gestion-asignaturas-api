package com.gestionacademica.infrastructure.persistence.adapter;

import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.application.service.EmailUsuarioDuplicadoException;
import com.gestionacademica.domain.model.Usuario;
import com.gestionacademica.infrastructure.persistence.mapper.UsuarioMapper;
import com.gestionacademica.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {
    private final SpringDataUsuarioRepository repository;

    public UsuarioRepositoryAdapter(SpringDataUsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        try {
            return UsuarioMapper.toDomain(repository.save(UsuarioMapper.toEntity(usuario)));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailUsuarioDuplicadoException();
        }
    }

    @Override public List<Usuario> listar() { return repository.findAll().stream().map(UsuarioMapper::toDomain).toList(); }
    @Override public Optional<Usuario> buscarPorId(Long id) { return repository.findById(id).map(UsuarioMapper::toDomain); }
    @Override public Optional<Usuario> buscarPorEmail(String email) { return repository.findByEmail(email).map(UsuarioMapper::toDomain); }
    @Override public boolean existePorRol(RolUsuario role) { return repository.existsByRole(role); }
    @Override public Optional<Usuario> buscarPorHashTokenRecuperacion(String hash) { return repository.findByResetTokenHash(hash).map(UsuarioMapper::toDomain); }
    @Override
    @Transactional
    public boolean restablecerPasswordSiTokenVigente(String hash, Instant ahora, String nuevoPasswordHash) {
        return repository.updatePasswordForValidResetToken(hash, ahora, nuevoPasswordHash, EstadoCuenta.ACTIVO) == 1;
    }
    @Override public void eliminarPorId(Long id) { repository.deleteById(id); }
}
