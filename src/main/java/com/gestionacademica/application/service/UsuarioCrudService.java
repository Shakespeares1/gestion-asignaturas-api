package com.gestionacademica.application.service;

import com.gestionacademica.application.port.input.ActualizarUsuarioUseCase;
import com.gestionacademica.application.port.input.CrearUsuarioUseCase;
import com.gestionacademica.application.port.input.EliminarUsuarioUseCase;
import com.gestionacademica.application.port.input.ListarUsuariosUseCase;
import com.gestionacademica.application.port.input.ObtenerUsuarioUseCase;
import com.gestionacademica.application.port.output.PasswordHasherPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.Usuario;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UsuarioCrudService implements CrearUsuarioUseCase, ListarUsuariosUseCase,
        ObtenerUsuarioUseCase, ActualizarUsuarioUseCase, EliminarUsuarioUseCase {
    private final UsuarioRepositoryPort repository;
    private final PasswordHasherPort passwordHasher;
    private final Clock clock;

    public UsuarioCrudService(UsuarioRepositoryPort repository, PasswordHasherPort passwordHasher, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Usuario crear(String username, String nombreCompleto, String email, String password) {
        if (!PasswordPolicy.isValid(password)) throw new ContrasenaInvalidaException(PasswordPolicy.validationMessage());
        String normalizedUsername = Usuario.normalizarUsername(username);
        String normalizedEmail = Usuario.normalizarEmail(email);
        if (repository.buscarPorUsername(normalizedUsername).isPresent()) {
            throw new UsernameUsuarioDuplicadoException();
        }
        if (repository.buscarPorEmail(normalizedEmail).isPresent()) throw new EmailUsuarioDuplicadoException();

        String hash = passwordHasher.hash(password);
        if (hash == null || hash.isBlank() || hash.equals(password)) {
            throw new IllegalStateException("El servicio de hash no produjo un hash seguro");
        }
        return repository.guardar(Usuario.registrar(
                normalizedUsername, nombreCompleto, normalizedEmail, hash, clock.instant()));
    }

    @Override public List<Usuario> listar() { return repository.listar(); }
    @Override public Optional<Usuario> obtener(Long id) { return repository.buscarPorId(id); }

    @Override
    public Usuario actualizar(Long id, String username, String nombreCompleto, String email) {
        Usuario existing = repository.buscarPorId(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
        String normalizedUsername = username == null ? existing.getUsername() : Usuario.normalizarUsername(username);
        Usuario updated = existing.actualizarDatos(normalizedUsername, nombreCompleto, email);

        Optional<Usuario> usernameOwner = repository.buscarPorUsername(updated.getUsername());
        if (usernameOwner.isPresent() && !usernameOwner.get().getId().equals(existing.getId())) {
            throw new UsernameUsuarioDuplicadoException();
        }
        Optional<Usuario> emailOwner = repository.buscarPorEmail(updated.getEmail());
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(existing.getId())) {
            throw new EmailUsuarioDuplicadoException();
        }
        return repository.guardar(updated);
    }

    @Override
    public boolean eliminar(Long id) {
        if (repository.buscarPorId(id).isEmpty()) return false;
        repository.eliminarPorId(id);
        return true;
    }
}
