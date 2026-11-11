package com.gestionacademica.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gestionacademica.application.port.output.AdminBootstrapLockPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BootstrapAdminServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Test
    void creaUnAdminActivoConEmailNormalizadoYHashBCryptProporcionadoPorElPuerto() {
        MemoryUsers users = new MemoryUsers();
        List<String> events = new ArrayList<>();
        BootstrapAdminService service = new BootstrapAdminService(users,
                raw -> { assertEquals("one-time-input", raw); events.add("hash"); return "{bcrypt}encoded"; },
                () -> events.add("lock"), Clock.fixed(NOW, ZoneOffset.UTC));

        Usuario created = service.crearPrimerAdministrador("Ada Lovelace", " ADA@Example.org ", "one-time-input");

        assertEquals(List.of("lock", "hash"), events);
        assertEquals("ada@example.org", created.getEmail());
        assertEquals(RolUsuario.ADMIN, created.getRole());
        assertEquals(EstadoCuenta.ACTIVO, created.getAccountStatus());
        assertEquals("{bcrypt}encoded", created.getPasswordHash());
        assertNotEquals("one-time-input", created.getPasswordHash());
        assertEquals(NOW, created.getFechaCreacion());
    }

    @Test
    void rehusaCrearUnSegundoAdministradorYBloqueaAntesDeComprobarlo() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(new Usuario(null, "Existing", "existing@example.org", "{bcrypt}hash",
                RolUsuario.ADMIN, EstadoCuenta.ACTIVO, NOW, null, null));
        List<String> events = new ArrayList<>();
        BootstrapAdminService service = new BootstrapAdminService(users, raw -> {
            throw new AssertionError("No debe hashear cuando ya existe un ADMIN");
        }, () -> events.add("lock"), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(AdminBootstrapAlreadyCompletedException.class,
                () -> service.crearPrimerAdministrador("Another", "another@example.org", "long-valid-password"));
        assertEquals(List.of("lock"), events);
        assertEquals(1, users.listar().stream().filter(user -> user.getRole() == RolUsuario.ADMIN).count());
    }

    private static final class MemoryUsers implements UsuarioRepositoryPort {
        private final List<Usuario> users = new ArrayList<>();
        private long nextId = 1;
        @Override public Usuario guardar(Usuario user) {
            Usuario saved = new Usuario(user.getId() == null ? nextId++ : user.getId(), user.getNombreCompleto(),
                    user.getEmail(), user.getPasswordHash(), user.getRole(), user.getAccountStatus(),
                    user.getFechaCreacion(), user.getResetTokenHash(), user.getResetTokenExpiry());
            users.add(saved);
            return saved;
        }
        @Override public List<Usuario> listar() { return List.copyOf(users); }
        @Override public Optional<Usuario> buscarPorId(Long id) { return users.stream().filter(u -> id.equals(u.getId())).findFirst(); }
        @Override public Optional<Usuario> buscarPorEmail(String email) { return users.stream().filter(u -> u.getEmail().equals(email)).findFirst(); }
        @Override public boolean existePorRol(RolUsuario role) { return users.stream().anyMatch(u -> u.getRole() == role); }
        @Override public Optional<Usuario> buscarPorHashTokenRecuperacion(String hash) { return Optional.empty(); }
        @Override public boolean restablecerPasswordSiTokenVigente(String hash, Instant now, String newHash) { return false; }
        @Override public void eliminarPorId(Long id) { users.removeIf(u -> id.equals(u.getId())); }
    }
}
