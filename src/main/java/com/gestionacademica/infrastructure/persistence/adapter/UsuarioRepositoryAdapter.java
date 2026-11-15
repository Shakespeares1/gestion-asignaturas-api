package com.gestionacademica.infrastructure.persistence.adapter;

import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.application.service.EmailUsuarioDuplicadoException;
import com.gestionacademica.application.service.UsernameUsuarioDuplicadoException;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import com.gestionacademica.infrastructure.persistence.entity.UsuarioEntity;
import com.gestionacademica.infrastructure.persistence.mapper.UsuarioMapper;
import com.gestionacademica.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {
    private final SpringDataUsuarioRepository repository;

    public UsuarioRepositoryAdapter(SpringDataUsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        try {
            return UsuarioMapper.toDomain(repository.saveAndFlush(UsuarioMapper.toEntity(usuario)));
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, "uk_usuarios_username")) {
                throw new UsernameUsuarioDuplicadoException();
            }
            if (hasConstraint(exception, "uk_usuarios_email")) {
                throw new EmailUsuarioDuplicadoException();
            }
            throw exception;
        }
    }

    private static boolean hasConstraint(Throwable throwable, String expectedName) {
        String expected = expectedName.toLowerCase(java.util.Locale.ROOT);
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null
                    && violation.getConstraintName().toLowerCase(java.util.Locale.ROOT).contains(expected)) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains(expected)) {
                return true;
            }
        }
        return false;
    }

    @Override public List<Usuario> listar() {
        return repository.findAll().stream().map(UsuarioMapper::toDomain).toList();
    }
    @Override public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id).map(UsuarioMapper::toDomain);
    }
    @Override public Optional<Usuario> buscarPorEmail(String email) {
        return repository.findByEmail(email).map(UsuarioMapper::toDomain);
    }
    @Override public Optional<Usuario> buscarPorUsername(String username) {
        return repository.findByUsername(username).map(UsuarioMapper::toDomain);
    }
    @Override public boolean existePorRol(RolUsuario role) { return repository.existsByRole(role); }
    @Override public Optional<Usuario> buscarPorHashTokenRecuperacion(String hash) {
        return repository.findByResetTokenHash(hash).map(UsuarioMapper::toDomain);
    }
    @Override
    @Transactional
    public boolean restablecerPasswordSiTokenVigente(String hash, Instant ahora, String nuevoPasswordHash) {
        return repository.updatePasswordForValidResetToken(hash, ahora, nuevoPasswordHash, EstadoCuenta.ACTIVO) == 1;
    }
    @Override public void eliminarPorId(Long id) { repository.deleteById(id); }
}
