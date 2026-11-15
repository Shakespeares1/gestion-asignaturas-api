package com.gestionacademica.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsuarioTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void normalizaUsernameYCorreoYPermiteCamposDeRecuperacionNulos() {
        Usuario usuario = new Usuario(null, "Ada.Lovelace", " Ada Lovelace ",
                "  ADA@Example.ORG ", "{bcrypt}hash", RolUsuario.USER,
                EstadoCuenta.ACTIVO, CREATED, null, null);

        assertEquals("ada.lovelace", usuario.getUsername());
        assertEquals("Ada Lovelace", usuario.getNombreCompleto());
        assertEquals("ada@example.org", usuario.getEmail());
        assertNull(usuario.getResetTokenHash());
        assertNull(usuario.getResetTokenExpiry());
    }

    @Test
    void usernameEsObligatorioYValidaFormatoYLongitud() {
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername(null));
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername("  "));
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername("ab"));
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername(".abc"));
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername("abc."));
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername("user@host"));
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername(" user "));
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername("user name"));
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarUsername("a".repeat(31)));
    }

    @Test
    void normalizaUsernameSinDistinguirMayusculas() {
        assertEquals("student_01", Usuario.normalizarUsername("Student_01"));
    }

    @Test
    void rechazaEmailYHashInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> usuario("sin-correo", "{bcrypt}hash"));
        assertThrows(IllegalArgumentException.class, () -> usuario("user@example.org", " "));
    }

    @Test
    void exigeHashYExpiracionDeRecuperacionJuntos() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario(null, "ada", "Ada", "ada@example.org",
                "{bcrypt}hash", RolUsuario.USER, EstadoCuenta.ACTIVO, CREATED,
                "token-hash", null));
        assertThrows(IllegalArgumentException.class, () -> new Usuario(null, "ada", "Ada", "ada@example.org",
                "{bcrypt}hash", RolUsuario.USER, EstadoCuenta.ACTIVO, CREATED,
                null, CREATED.plusSeconds(60)));
    }

    @Test
    void registrarUsaRolYEstadoInicialesSeguros() {
        Usuario usuario = Usuario.registrar("ada", "Ada", "ada@example.org", "{bcrypt}hash", CREATED);

        assertEquals("ada", usuario.getUsername());
        assertEquals(RolUsuario.USER, usuario.getRole());
        assertEquals(EstadoCuenta.ACTIVO, usuario.getAccountStatus());
        assertEquals(CREATED, usuario.getFechaCreacion());
    }

    private static Usuario usuario(String email, String hash) {
        return new Usuario(null, "ada", "Ada", email, hash, RolUsuario.USER,
                EstadoCuenta.ACTIVO, CREATED, null, null);
    }
}
