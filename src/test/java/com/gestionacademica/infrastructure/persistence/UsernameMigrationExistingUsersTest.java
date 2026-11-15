package com.gestionacademica.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class UsernameMigrationExistingUsersTest {
    @Test
    void backfillsExistingUsersWithoutRemovingRowsOrUsingPersonalData() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:username-migration-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Flyway.configure()
                    .dataSource(jdbcUrl, "sa", "")
                    .locations("classpath:db/migration")
                    .target(MigrationVersion.fromVersion("2"))
                    .load()
                    .migrate();

            try (Statement statement = connection.createStatement()) {
                // H2 can retain a closed in-memory database reference inside V2's CHECK expression.
                // Drop only that unrelated test constraint; production migration V3 is tested unchanged.
                statement.execute("alter table usuarios drop constraint ck_usuarios_role_valido");
                statement.execute("alter table usuarios drop constraint ck_usuarios_account_status_valido");
                statement.execute("alter table usuarios drop constraint ck_usuarios_reset_token_pair");
                statement.executeUpdate("insert into usuarios "
                        + "(id, nombre_completo, email, password_hash, user_role, account_status, fecha_creacion) "
                        + "values (101, 'Existing User One', 'one@example.org', 'hash-one', 'USER', 'ACTIVO', "
                        + "timestamp with time zone '2026-01-01 00:00:00+00:00')");
                statement.executeUpdate("insert into usuarios "
                        + "(id, nombre_completo, email, password_hash, user_role, account_status, fecha_creacion) "
                        + "values (202, 'Existing User Two', 'two@example.org', 'hash-two', 'ADMIN', 'ACTIVO', "
                        + "timestamp with time zone '2026-01-02 00:00:00+00:00')");
            }

            Flyway.configure()
                    .dataSource(jdbcUrl, "sa", "")
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery(
                            "select id, username from usuarios order by id")) {
                assertEquals(2, rowCount(connection));
                rows.next();
                assertEquals(101L, rows.getLong("id"));
                assertEquals("usuario_101", rows.getString("username"));
                rows.next();
                assertEquals(202L, rows.getLong("id"));
                assertEquals("usuario_202", rows.getString("username"));
            }
        }
    }

    private static int rowCount(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select count(*) from usuarios")) {
            result.next();
            return result.getInt(1);
        }
    }
}
