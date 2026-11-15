package com.gestionacademica.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.application.service.EmailUsuarioDuplicadoException;
import com.gestionacademica.application.service.UsernameUsuarioDuplicadoException;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import com.gestionacademica.infrastructure.security.TestSecurityBeans;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class UsuarioPersistenceIsolationTest {
    @Autowired private DataSource dataSource;
    @Autowired private UsuarioRepositoryPort usuarioRepository;

    @Test
    void perfilTestUsaH2EnMemoria() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
    }

    @Test
    @Transactional
    void persisteUsernameCanonicoYPermiteBuscarlo() {
        Usuario saved = usuarioRepository.guardar(Usuario.registrar(
                "Ada.User", "Ada User", "ada@example.org", "{bcrypt}test-hash",
                Instant.parse("2026-01-01T00:00:00Z")));

        assertEquals("ada.user", saved.getUsername());
        assertEquals("ada.user", usuarioRepository.buscarPorUsername("ada.user").orElseThrow().getUsername());
    }

    @Test
    @Transactional
    void persistenciaRechazaCorreoDuplicadoPorRestriccionUnica() {
        usuarioRepository.guardar(Usuario.registrar("duplicate-one", "Usuario uno", "duplicate@example.org",
                "{bcrypt}test-hash-one", Instant.parse("2026-01-01T00:00:00Z")));

        assertThrows(EmailUsuarioDuplicadoException.class,
                () -> usuarioRepository.guardar(Usuario.registrar("duplicate-two", "Usuario dos",
                        "duplicate@example.org", "{bcrypt}test-hash-two",
                        Instant.parse("2026-01-02T00:00:00Z"))));
    }

    @Test
    @Transactional
    void persistenciaRechazaUsernameDuplicadoAunqueCambienMayusculas() {
        usuarioRepository.guardar(Usuario.registrar("Case.User", "Usuario uno", "case-one@example.org",
                "{bcrypt}test-hash-one", Instant.parse("2026-01-01T00:00:00Z")));

        assertThrows(UsernameUsuarioDuplicadoException.class,
                () -> usuarioRepository.guardar(Usuario.registrar("case.user", "Usuario dos",
                        "case-two@example.org", "{bcrypt}test-hash-two",
                        Instant.parse("2026-01-02T00:00:00Z"))));
    }

    @Test
    @Transactional
    void actualizacionCondicionalConsumeUnaSolaVezTokenDeRecuperacion() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        Usuario saved = usuarioRepository.guardar(new Usuario(null, "reset-user", "User",
                "reset@example.org", "old-hash", RolUsuario.USER, EstadoCuenta.ACTIVO,
                now, "stored-token-hash", now.plusSeconds(60)));

        assertTrue(usuarioRepository.restablecerPasswordSiTokenVigente("stored-token-hash", now, "new-hash"));
        assertFalse(usuarioRepository.restablecerPasswordSiTokenVigente("stored-token-hash", now, "other-hash"));
        Usuario updated = usuarioRepository.buscarPorId(saved.getId()).orElseThrow();
        assertEquals("new-hash", updated.getPasswordHash());
        assertEquals("reset-user", updated.getUsername());
        assertNull(updated.getResetTokenHash());
        assertNull(updated.getResetTokenExpiry());
    }
}
