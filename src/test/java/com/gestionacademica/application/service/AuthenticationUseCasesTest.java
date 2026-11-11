package com.gestionacademica.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.application.port.output.JwtIssuerPort;
import com.gestionacademica.application.port.output.PasswordResetEmailPort;
import com.gestionacademica.application.port.output.PasswordResetTokenPort;
import com.gestionacademica.application.port.output.PasswordVerifierPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthenticationUseCasesTest {
    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void loginCorrectoDevuelveTokenYDatosPublicos() {
        MemoryUsers users = new MemoryUsers();
        Usuario user = users.guardar(user(1L, EstadoCuenta.ACTIVO, RolUsuario.ADMIN, null, null));
        PasswordVerifierPort verifier = (raw, hash) -> raw.equals("correct-password") && hash.equals("stored-hash");
        JwtIssuerPort issuer = found -> new JwtIssuerPort.IssuedToken("signed-jwt", NOW.plusSeconds(300));

        var result = new LoginService(users, verifier, issuer).login(" USER@example.org ", "correct-password");

        assertEquals("signed-jwt", result.token().value());
        assertEquals(user.getId(), result.userId());
        assertEquals("ADMIN", result.role());
    }

    @Test
    void loginRechazaPasswordIncorrectoYUsuarioInexistenteSinRevelarCualFallo() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(user(1L, EstadoCuenta.ACTIVO, RolUsuario.USER, null, null));
        LoginService service = new LoginService(users, (raw, hash) -> false,
                found -> new JwtIssuerPort.IssuedToken("unused", NOW.plusSeconds(60)));

        assertThrows(CredencialesInvalidasException.class, () -> service.login("user@example.org", "wrong"));
        assertThrows(CredencialesInvalidasException.class, () -> service.login("missing@example.org", "wrong"));
    }

    @Test
    void loginRechazaUsuarioInactivo() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(user(1L, EstadoCuenta.INACTIVO, RolUsuario.USER, null, null));
        LoginService service = new LoginService(users, (raw, hash) -> true,
                found -> new JwtIssuerPort.IssuedToken("unused", NOW.plusSeconds(60)));
        assertThrows(CuentaInactivaException.class, () -> service.login("user@example.org", "correct"));
    }

    @Test
    void loginMantieneCompatibilidadConContrasenaHistoricaMenorA12Caracteres() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(user(1L, EstadoCuenta.ACTIVO, RolUsuario.USER, null, null));
        LoginService service = new LoginService(users, (raw, hash) -> "old".equals(raw),
                found -> new JwtIssuerPort.IssuedToken("legacy-jwt", NOW.plusSeconds(60)));
        assertEquals("legacy-jwt", service.login("user@example.org", "old").token().value());
    }

    @Test
    void solicitudRecuperacionNoRevelaCorreoInexistenteYNoGeneraToken() {
        MemoryUsers users = new MemoryUsers();
        TestTokens tokens = new TestTokens();
        TestEmail email = new TestEmail(true);
        ForgotPasswordService service = new ForgotPasswordService(users, tokens, email, CLOCK);

        assertDoesNotThrow(() -> service.requestReset("missing@example.org"));
        assertEquals(0, tokens.generated.size());
        assertTrue(email.sent.isEmpty());
    }

    @Test
    void correoNoConfiguradoSeInformaSinConsultarExistencia() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(user(1L, EstadoCuenta.ACTIVO, RolUsuario.USER, null, null));
        ForgotPasswordService service = new ForgotPasswordService(users, new TestTokens(), new TestEmail(false), CLOCK);

        assertThrows(CorreoNoDisponibleException.class, () -> service.requestReset("user@example.org"));
        assertEquals(0, users.emailLookups);
    }

    @Test
    void solicitudValidaGuardaSoloHashYEntregaTokenCrudoAlPuertoDeCorreo() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(user(1L, EstadoCuenta.ACTIVO, RolUsuario.USER, null, null));
        TestTokens tokens = new TestTokens();
        TestEmail email = new TestEmail(true);

        new ForgotPasswordService(users, tokens, email, CLOCK).requestReset("USER@example.org");

        Usuario saved = users.buscarPorEmail("user@example.org").orElseThrow();
        assertEquals("hash:raw-reset-token", saved.getResetTokenHash());
        assertEquals(NOW.plusSeconds(1800), saved.getResetTokenExpiry());
        assertEquals("raw-reset-token", email.sent.getFirst());
    }

    @Test
    void entregaFallidaInvalidaTokenESigueSinRevelarExistencia() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(user(1L, EstadoCuenta.ACTIVO, RolUsuario.USER, null, null));
        TestEmail email = new TestEmail(true);
        email.fail = true;

        assertDoesNotThrow(() -> new ForgotPasswordService(users, new TestTokens(), email, CLOCK)
                .requestReset("user@example.org"));
        Usuario saved = users.buscarPorEmail("user@example.org").orElseThrow();
        assertNull(saved.getResetTokenHash());
        assertNull(saved.getResetTokenExpiry());
    }

    @Test
    void restablecePasswordConTokenValidoLoInvalidaYRechazaSuReutilizacion() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(user(1L, EstadoCuenta.ACTIVO, RolUsuario.USER, "hash:raw", NOW.plusSeconds(60)));
        TestTokens tokens = new TestTokens();
        ResetPasswordService service = new ResetPasswordService(users, tokens, password -> "{bcrypt}new-hash", CLOCK);

        assertTrue(service.reset("raw", "new-password"));
        assertEquals("{bcrypt}new-hash", users.buscarPorEmail("user@example.org").orElseThrow().getPasswordHash());
        assertNull(users.buscarPorEmail("user@example.org").orElseThrow().getResetTokenHash());
        assertFalse(service.reset("raw", "another-password"));
    }

    @Test
    void rechazaTokenExpiradoYNoAlteraPassword() {
        MemoryUsers users = new MemoryUsers();
        users.guardar(user(1L, EstadoCuenta.ACTIVO, RolUsuario.USER, "hash:raw", NOW.minusSeconds(1)));
        ResetPasswordService service = new ResetPasswordService(users, new TestTokens(), password -> "{bcrypt}new", CLOCK);

        assertFalse(service.reset("raw", "new-password"));
        assertEquals("stored-hash", users.buscarPorEmail("user@example.org").orElseThrow().getPasswordHash());
    }

    @Test
    void restablecimientoRechazaContrasenaSobreElLimiteBcrypt() {
        ResetPasswordService service = new ResetPasswordService(new MemoryUsers(), new TestTokens(), password -> password, CLOCK);
        assertThrows(ContrasenaResetInvalidaException.class, () -> service.reset("token", "x".repeat(73)));
        assertThrows(ContrasenaResetInvalidaException.class, () -> service.reset("token", "short-pass"));
    }

    private static Usuario user(Long id, EstadoCuenta state, RolUsuario role, String tokenHash, Instant expiry) {
        return new Usuario(id, "Test User", "user@example.org", "stored-hash", role, state, NOW, tokenHash, expiry);
    }

    private static final class TestTokens implements PasswordResetTokenPort {
        final List<String> generated = new ArrayList<>();
        @Override public GeneratedToken generate() {
            generated.add("raw-reset-token");
            return new GeneratedToken("raw-reset-token", "hash:raw-reset-token");
        }
        @Override public String hash(String rawToken) { return "hash:" + rawToken; }
    }

    private static final class TestEmail implements PasswordResetEmailPort {
        final boolean available;
        final List<String> sent = new ArrayList<>();
        boolean fail;
        TestEmail(boolean available) { this.available = available; }
        @Override public boolean isAvailable() { return available; }
        @Override public void sendResetLink(String recipient, String token, Instant expiry) {
            if (fail) throw new IllegalStateException("simulated transport failure");
            sent.add(token);
        }
    }

    private static final class MemoryUsers implements UsuarioRepositoryPort {
        final Map<Long, Usuario> byId = new HashMap<>();
        long nextId = 1;
        int emailLookups;
        @Override public Usuario guardar(Usuario user) {
            Usuario saved = new Usuario(user.getId() == null ? nextId++ : user.getId(), user.getNombreCompleto(),
                    user.getEmail(), user.getPasswordHash(), user.getRole(), user.getAccountStatus(),
                    user.getFechaCreacion(), user.getResetTokenHash(), user.getResetTokenExpiry());
            byId.put(saved.getId(), saved);
            return saved;
        }
        @Override public List<Usuario> listar() { return List.copyOf(byId.values()); }
        @Override public Optional<Usuario> buscarPorId(Long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<Usuario> buscarPorEmail(String email) {
            emailLookups++;
            return byId.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }
        @Override public boolean existePorRol(RolUsuario role) {
            return byId.values().stream().anyMatch(user -> user.getRole() == role);
        }
        @Override public Optional<Usuario> buscarPorHashTokenRecuperacion(String hash) {
            return byId.values().stream().filter(user -> hash.equals(user.getResetTokenHash())).findFirst();
        }
        @Override public boolean restablecerPasswordSiTokenVigente(String hash, Instant now, String newHash) {
            Usuario user = buscarPorHashTokenRecuperacion(hash).orElse(null);
            if (user == null || user.getAccountStatus() != EstadoCuenta.ACTIVO
                    || user.getResetTokenExpiry() == null || !user.getResetTokenExpiry().isAfter(now)) return false;
            byId.put(user.getId(), new Usuario(user.getId(), user.getNombreCompleto(), user.getEmail(), newHash,
                    user.getRole(), user.getAccountStatus(), user.getFechaCreacion(), null, null));
            return true;
        }
        @Override public void eliminarPorId(Long id) { byId.remove(id); }
    }
}
