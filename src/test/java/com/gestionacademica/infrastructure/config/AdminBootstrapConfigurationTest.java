package com.gestionacademica.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AdminBootstrapConfigurationTest {
    @Test
    void exigeEjecucionNoWebSinMigracionesNiInicializacionDeEsquema() {
        MockEnvironment environment = validEnvironment();
        assertDoesNotThrow(() -> AdminBootstrapConfiguration.validateBootstrapExecution(environment));

        MockEnvironment flywayEnabled = validEnvironment();
        flywayEnabled.setProperty("spring.flyway.enabled", "true");
        assertThrows(IllegalStateException.class,
                () -> AdminBootstrapConfiguration.validateBootstrapExecution(flywayEnabled));

        MockEnvironment web = validEnvironment();
        web.setProperty("spring.main.web-application-type", "servlet");
        assertThrows(IllegalStateException.class,
                () -> AdminBootstrapConfiguration.validateBootstrapExecution(web));

        MockEnvironment schemaUpdate = validEnvironment();
        schemaUpdate.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        assertThrows(IllegalStateException.class,
                () -> AdminBootstrapConfiguration.validateBootstrapExecution(schemaUpdate));

        MockEnvironment sqlInit = validEnvironment();
        sqlInit.setProperty("spring.sql.init.mode", "always");
        assertThrows(IllegalStateException.class,
                () -> AdminBootstrapConfiguration.validateBootstrapExecution(sqlInit));
    }

    private static MockEnvironment validEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("admin-bootstrap");
        environment.setProperty("spring.main.web-application-type", "none");
        environment.setProperty("spring.flyway.enabled", "false");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        environment.setProperty("spring.sql.init.mode", "never");
        environment.setProperty("DB_URL", "jdbc:postgresql://db.example.org:5432/app?sslmode=require");
        environment.setProperty("DB_USERNAME", "test-user");
        environment.setProperty("DB_PASSWORD", "test-only-placeholder");
        return environment;
    }
}
