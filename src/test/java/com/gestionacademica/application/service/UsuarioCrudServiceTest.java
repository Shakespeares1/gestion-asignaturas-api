package com.gestionacademica.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.application.port.output.PasswordHasherPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsuarioCrudServiceTest {
    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private RepositorioEnMemoria repository;
    private UsuarioCrudService service;

    @BeforeEach
    void setUp() {
        repository = new RepositorioEnMemoria();
        service = new UsuarioCrudService(repository, password -> "{test}hash-" + password,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void creaUsuarioNormalizandoEmailYGuardandoSoloHash() {
        Usuario created = service.crear("Ada Lovelace", " ADA@Example.org ", "plain-test-input");

        assertEquals("ada@example.org", created.getEmail());
        assertEquals("{test}hash-plain-test-input", created.getPasswordHash());
        assertNotEquals("plain-test-input", created.getPasswordHash());
        assertEquals(RolUsuario.USER, created.getRole());
        assertEquals(EstadoCuenta.ACTIVO, created.getAccountStatus());
        assertEquals(NOW, created.getFechaCreacion());
    }

    @Test
    void rechazaEmailDuplicadoSinProcesarLaContrasena() {
        repository.guardar(usuario(null, "user@example.org", "{bcrypt}existing"));
        AtomicBoolean hasherCalled = new AtomicBoolean();
        service = new UsuarioCrudService(repository, password -> {
            hasherCalled.set(true);
            return "{test}hash";
        }, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(EmailUsuarioDuplicadoException.class,
                () -> service.crear("Otra persona", " USER@example.org ", "valid-long-password"));
        assertFalse(hasherCalled.get());
    }

    @Test
    void rechazaContrasenaCortaAntesDeCalcularHash() {
        AtomicBoolean hasherCalled = new AtomicBoolean();
        service = new UsuarioCrudService(repository, password -> {
            hasherCalled.set(true);
            return "{test}hash";
        }, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(ContrasenaInvalidaException.class,
                () -> service.crear("Nombre", "new@example.org", "short-pass"));
        assertFalse(hasherCalled.get());
    }

    @Test
    void permiteConsultarYListarUsuarios() {
        Usuario saved = repository.guardar(usuario(null, "user@example.org", "{bcrypt}hash"));

        assertEquals(List.of(saved), service.listar());
        assertEquals(Optional.of(saved), service.obtener(saved.getId()));
        assertTrue(service.obtener(999L).isEmpty());
    }

    @Test
    void actualizaSoloDatosEditablesYConservaCamposProtegidos() {
        Instant expiry = NOW.plusSeconds(600);
        Usuario saved = repository.guardar(new Usuario(null, "Nombre anterior", "user@example.org",
                "{bcrypt}secret-hash", RolUsuario.ADMIN, EstadoCuenta.INACTIVO, NOW,
                "reset-hash", expiry));

        Usuario updated = service.actualizar(saved.getId(), "Nombre nuevo", " NEW@example.org ");

        assertEquals("Nombre nuevo", updated.getNombreCompleto());
        assertEquals("new@example.org", updated.getEmail());
        assertEquals("{bcrypt}secret-hash", updated.getPasswordHash());
        assertEquals(RolUsuario.ADMIN, updated.getRole());
        assertEquals(EstadoCuenta.INACTIVO, updated.getAccountStatus());
        assertEquals(NOW, updated.getFechaCreacion());
        assertEquals("reset-hash", updated.getResetTokenHash());
        assertEquals(expiry, updated.getResetTokenExpiry());
    }

    @Test
    void rechazaCorreoDuplicadoAlActualizar() {
        Usuario first = repository.guardar(usuario(null, "first@example.org", "{bcrypt}one"));
        repository.guardar(usuario(null, "second@example.org", "{bcrypt}two"));

        assertThrows(EmailUsuarioDuplicadoException.class,
                () -> service.actualizar(first.getId(), "Nombre", "SECOND@example.org"));
    }

    @Test
    void eliminaUsuarioExistenteYReportaCuandoNoExiste() {
        Usuario saved = repository.guardar(usuario(null, "user@example.org", "{bcrypt}hash"));

        assertTrue(service.eliminar(saved.getId()));
        assertTrue(service.obtener(saved.getId()).isEmpty());
        assertFalse(service.eliminar(saved.getId()));
    }

    @Test
    void actualizacionDeUsuarioInexistenteFallaConExcepcionDeDominioDeAplicacion() {
        assertThrows(UsuarioNoEncontradoException.class,
                () -> service.actualizar(999L, "Nombre", "user@example.org"));
    }

    private static Usuario usuario(Long id, String email, String hash) {
        return new Usuario(id, "Usuario de prueba", email, hash, RolUsuario.USER,
                EstadoCuenta.ACTIVO, NOW, null, null);
    }

    private static final class RepositorioEnMemoria implements UsuarioRepositoryPort {
        private final Map<Long, Usuario> usuarios = new HashMap<>();
        private long nextId = 1;

        @Override
        public Usuario guardar(Usuario usuario) {
            Usuario saved = new Usuario(usuario.getId() == null ? nextId++ : usuario.getId(),
                    usuario.getNombreCompleto(), usuario.getEmail(), usuario.getPasswordHash(),
                    usuario.getRole(), usuario.getAccountStatus(), usuario.getFechaCreacion(),
                    usuario.getResetTokenHash(), usuario.getResetTokenExpiry());
            usuarios.put(saved.getId(), saved);
            return saved;
        }

        @Override public List<Usuario> listar() { return List.copyOf(usuarios.values()); }
        @Override public Optional<Usuario> buscarPorId(Long id) { return Optional.ofNullable(usuarios.get(id)); }
        @Override public Optional<Usuario> buscarPorEmail(String email) {
            return usuarios.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }
        @Override public boolean existePorRol(RolUsuario role) {
            return usuarios.values().stream().anyMatch(user -> user.getRole() == role);
        }
        @Override public Optional<Usuario> buscarPorHashTokenRecuperacion(String tokenHash) {
            return usuarios.values().stream().filter(user -> tokenHash.equals(user.getResetTokenHash())).findFirst();
        }
        @Override public boolean restablecerPasswordSiTokenVigente(String tokenHash, Instant ahora, String nuevoPasswordHash) {
            Usuario user = buscarPorHashTokenRecuperacion(tokenHash).orElse(null);
            if (user == null || user.getResetTokenExpiry() == null || !user.getResetTokenExpiry().isAfter(ahora)) return false;
            usuarios.put(user.getId(), new Usuario(user.getId(), user.getNombreCompleto(), user.getEmail(),
                    nuevoPasswordHash, user.getRole(), user.getAccountStatus(), user.getFechaCreacion(), null, null));
            return true;
        }
        @Override public void eliminarPorId(Long id) { usuarios.remove(id); }
    }
}
