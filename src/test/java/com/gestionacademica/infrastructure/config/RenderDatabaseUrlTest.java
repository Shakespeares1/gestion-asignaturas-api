package com.gestionacademica.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RenderDatabaseUrlTest {
    @Test
    void convierteConnectionStringRenderYNoIncluyeCredencialesEnJdbcUrl() {
        String jdbcUrl = RenderDatabaseUrl.toJdbcUrl(
                "postgresql://app:secret-value@db.internal:5432/gestion?connectTimeout=5");

        assertEquals("jdbc:postgresql://db.internal:5432/gestion?connectTimeout=5&sslmode=require", jdbcUrl);
        assertFalse(jdbcUrl.contains("secret-value"));
    }

    @Test
    void exigeSslAunqueElConnectionStringDeclareSslDesactivado() {
        assertEquals("jdbc:postgresql://db.internal:5432/gestion?sslmode=require",
                RenderDatabaseUrl.toJdbcUrl("postgres://app:secret@db.internal:5432/gestion?sslmode=disable"));
    }

    @Test
    void conservaUrlJdbcYaConfigurada() {
        String jdbcUrl = "jdbc:postgresql://db.internal:5432/gestion?sslmode=require";
        assertEquals(jdbcUrl, RenderDatabaseUrl.toJdbcUrl(jdbcUrl));
    }

    @Test
    void rechazaConnectionStringSinHostOPuerto() {
        assertThrows(IllegalArgumentException.class,
                () -> RenderDatabaseUrl.toJdbcUrl("postgresql://app:secret@/gestion"));
    }
}
