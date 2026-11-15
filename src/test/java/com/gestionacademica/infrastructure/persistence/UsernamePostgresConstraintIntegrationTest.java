package com.gestionacademica.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class UsernamePostgresConstraintIntegrationTest {
    private static final String REQUIRED_URL = "jdbc:postgresql://127.0.0.1:55432/username_check";

    @Test
    void postgresConstraintEnforcesUsernameRuleAndPreservesBackfilledRows() throws Exception {
        String url = System.getenv("USERNAME_PG_TEST_URL");
        String user = System.getenv("USERNAME_PG_TEST_USER");
        String password = System.getenv("USERNAME_PG_TEST_PASSWORD");
        Assumptions.assumeTrue(REQUIRED_URL.equals(url) && user != null && !user.isBlank() && password != null,
                "Requiere propiedades de conexión exclusivas para PostgreSQL local aislado");
        String schema = "username_test_" + UUID.randomUUID().toString().replace("-", "");

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .schemas(schema)
                .defaultSchema(schema)
                .target(MigrationVersion.fromVersion("2"))
                .load()
                .migrate();

        String schemaUrl = url + "?currentSchema=" + schema;
        try (Connection connection = DriverManager.getConnection(schemaUrl, user, password)) {
            seedLegacyUser(connection, 101, "Legacy One", "legacy-one@example.org", "hash-one");
            seedLegacyUser(connection, 202, "Legacy Two", "legacy-two@example.org", "hash-two");

            Flyway.configure()
                    .dataSource(url, user, password)
                    .locations("classpath:db/migration")
                    .schemas(schema)
                    .defaultSchema(schema)
                    .load()
                    .migrate();

            assertLegacyRowsPreserved(connection);
            insertUser(connection, "valid.name_01-2", "valid@example.org");
            insertUser(connection, "abc", "minimum@example.org");
            insertUser(connection, "a".repeat(30), "maximum@example.org");
            assertRejected(connection, "Uppercase", "uppercase@example.org");
            assertRejected(connection, "has space", "space@example.org");
            assertRejected(connection, "bad$name", "invalid-char@example.org");
            assertRejected(connection, "ab", "short@example.org");
            assertRejected(connection, "a".repeat(31), "long@example.org");
            assertRejected(connection, "usuario_101", "duplicate@example.org");

            assertEquals(5, countUsers(connection),
                    "Solo debe persistir el username válido además de los dos usuarios previos");
            assertLegacyRowsPreserved(connection);
        }
    }

    private static void seedLegacyUser(Connection connection, long id, String name, String email, String hash)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into usuarios (id, nombre_completo, email, password_hash, user_role, account_status, fecha_creacion) "
                        + "values (?, ?, ?, ?, 'USER', 'ACTIVO', current_timestamp)")) {
            statement.setLong(1, id);
            statement.setString(2, name);
            statement.setString(3, email);
            statement.setString(4, hash);
            statement.executeUpdate();
        }
    }

    private static void assertLegacyRowsPreserved(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "select id, username, nombre_completo, email, password_hash from usuarios where id in (101, 202) order by id")) {
            assertEquals(true, rows.next());
            assertEquals(101L, rows.getLong("id"));
            assertEquals("usuario_101", rows.getString("username"));
            assertEquals("Legacy One", rows.getString("nombre_completo"));
            assertEquals("legacy-one@example.org", rows.getString("email"));
            assertEquals("hash-one", rows.getString("password_hash"));
            assertEquals(true, rows.next());
            assertEquals(202L, rows.getLong("id"));
            assertEquals("usuario_202", rows.getString("username"));
            assertEquals("Legacy Two", rows.getString("nombre_completo"));
            assertEquals("legacy-two@example.org", rows.getString("email"));
            assertEquals("hash-two", rows.getString("password_hash"));
        }
    }

    private static void insertUser(Connection connection, String username, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into usuarios (username, nombre_completo, email, password_hash, user_role, account_status, fecha_creacion) "
                        + "values (?, 'Test User', ?, 'test-hash', 'USER', 'ACTIVO', current_timestamp)")) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    private static void assertRejected(Connection connection, String username, String email) {
        assertThrows(SQLException.class, () -> insertUser(connection, username, email),
                "PostgreSQL debe rechazar directamente: " + username);
    }

    private static int countUsers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select count(*) from usuarios")) {
            result.next();
            return result.getInt(1);
        }
    }
}
