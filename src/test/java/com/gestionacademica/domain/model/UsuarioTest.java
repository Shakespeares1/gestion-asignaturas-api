package com.gestionacademica.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsuarioTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void normalizaCorreoYPermiteCamposDeRecuperacionNulos() {
        Usuario usuario = new Usuario(null, " Ada Lovelace ", "  ADA@Example.ORG ", "{bcrypt}hash",
                RolUsuario.USER, EstadoCuenta.ACTIVO, CREATED, null, null);

        assertEquals("Ada Lovelace", usuario.getNombreCompleto());
        assertEquals("ada@example.org", usuario.getEmail());
        assertNull(usuario.getResetTokenHash());
        assertNull(usuario.getResetTokenExpiry());
    }

    @Test
    void rechazaEmailYHashInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> usuario("sin-correo", "{bcrypt}hash"));
        assertThrows(IllegalArgumentException.class, () -> usuario("user@example.org", " "));
    }

    @Test
    void exigeHashYExpiracionDeRecuperacionJuntos() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario(null, "Ada", "ada@example.org",
                "{bcrypt}hash", RolUsuario.USER, EstadoCuenta.ACTIVO, CREATED,
                "token-hash", null));
        assertThrows(IllegalArgumentException.class, () -> new Usuario(null, "Ada", "ada@example.org",
                "{bcrypt}hash", RolUsuario.USER, EstadoCuenta.ACTIVO, CREATED,
                null, CREATED.plusSeconds(60)));
    }

    @Test
    void registrarUsaRolYEstadoInicialesSeguros() {
        Usuario usuario = Usuario.registrar("Ada", "ada@example.org", "{bcrypt}hash", CREATED);

        assertEquals(RolUsuario.USER, usuario.getRole());
        assertEquals(EstadoCuenta.ACTIVO, usuario.getAccountStatus());
        assertEquals(CREATED, usuario.getFechaCreacion());
    }

    private static Usuario usuario(String email, String hash) {
        return new Usuario(null, "Ada", email, hash, RolUsuario.USER, EstadoCuenta.ACTIVO, CREATED, null, null);
    }
}
