package com.gestionacademica.application.service;

import com.gestionacademica.application.port.input.BootstrapAdminUseCase;
import com.gestionacademica.application.port.output.AdminBootstrapLockPort;
import com.gestionacademica.application.port.output.PasswordHasherPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.Clock;
import java.util.Objects;

public final class BootstrapAdminService implements BootstrapAdminUseCase {
    private final UsuarioRepositoryPort usuarios;
    private final PasswordHasherPort passwordHasher;
    private final AdminBootstrapLockPort lock;
    private final Clock clock;

    public BootstrapAdminService(UsuarioRepositoryPort usuarios, PasswordHasherPort passwordHasher,
            AdminBootstrapLockPort lock, Clock clock) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.lock = Objects.requireNonNull(lock);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Usuario crearPrimerAdministrador(String nombreCompleto, String email, String password) {
        String normalizedEmail = Usuario.normalizarEmail(email);
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }
        if (!PasswordPolicy.isValid(password)) throw new IllegalArgumentException(PasswordPolicy.validationMessage());

        lock.bloquearInicializacion();
        if (usuarios.existePorRol(RolUsuario.ADMIN)) throw new AdminBootstrapAlreadyCompletedException();
        if (usuarios.buscarPorEmail(normalizedEmail).isPresent()) throw new EmailUsuarioDuplicadoException();

        String passwordHash = passwordHasher.hash(password);
        if (passwordHash == null || passwordHash.isBlank() || passwordHash.equals(password)) {
            throw new IllegalStateException("El servicio de hash no produjo un hash seguro");
        }
        return usuarios.guardar(Usuario.registrarAdministradorInicial(
                nombreCompleto, normalizedEmail, passwordHash, clock.instant()));
    }
}
