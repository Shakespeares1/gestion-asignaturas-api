package com.gestionacademica.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.gestionacademica.infrastructure.security.TestSecurityBeans;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class FlywayMigrationIntegrationTest {
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void ejecutaMigracionesVersionadasAntesDeLaValidacionJpa() {
        assertEquals("2", flyway.info().current().getVersion().getVersion());
        assertDoesNotThrow(flyway::validate);
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'ASIGNATURAS'", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'USUARIOS'", Integer.class));
    }

    @Test
    void migracionesAplicanRestriccionesDeIntegridadEsperadas() {
        String email = "migration-test-" + UUID.randomUUID() + "@example.org";
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "insert into asignaturas (nombre, nombre_completo, numero_creditos, semestre) values (?, ?, ?, ?)",
                "Bases de datos", "Bases de datos", 0, 1));
        jdbcTemplate.update(
                "insert into usuarios (nombre_completo, email, password_hash, user_role, account_status, fecha_creacion) "
                        + "values (?, ?, ?, ?, ?, current_timestamp)",
                "Usuario", email, "hash", "ADMIN", "ACTIVO");
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "insert into usuarios (nombre_completo, email, password_hash, user_role, account_status, fecha_creacion) "
                        + "values (?, ?, ?, ?, ?, current_timestamp)",
                "Otro", email, "hash", "USER", "ACTIVO"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "insert into usuarios (nombre_completo, email, password_hash, user_role, account_status, fecha_creacion, reset_token_hash) "
                        + "values (?, ?, ?, ?, ?, current_timestamp, ?)",
                "Token incompleto", "token-pair@example.org", "hash", "USER", "ACTIVO", "token-hash"));
    }
}
