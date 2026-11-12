package com.gestionacademica.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionEnvironmentGuardTest {
    @Test
    void perfilProdExponePortDinamicoYExigeTlsSinConectarAUnaBase() throws Exception {
        Properties production = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application-prod.properties")) {
            assertNotNull(input);
            production.load(input);
        }
        assertEquals("validate", production.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", production.getProperty("spring.flyway.enabled"));
        assertEquals("require", production.getProperty("spring.datasource.hikari.data-source-properties.sslmode"));
        Properties defaults = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(input);
            defaults.load(input);
        }
        assertEquals("${SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED:true}",
                defaults.getProperty("spring.mail.properties.mail.smtp.starttls.required"));
        assertEquals("${SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_CHECKSERVERIDENTITY:true}",
                defaults.getProperty("spring.mail.properties.mail.smtp.ssl.checkserveridentity"));
        assertEquals("${PORT:10000}", production.getProperty("server.port"));
        assertEquals("0.0.0.0", production.getProperty("server.address"));
        assertEquals("none", production.getProperty("server.forward-headers-strategy"));
    }

    @Test
    void perfilProdAceptaSoloConfiguracionValidaSinAbrirDatasource() {
        MockEnvironment environment = productionEnvironment();
        assertDoesNotThrow(() -> ProductionEnvironmentGuard.validateProductionEnvironment(environment));
    }

    @Test
    void prodRechazaUrlSinSsl() {
        MockEnvironment environment = productionEnvironment();
        environment.setProperty("DB_URL", "jdbc:postgresql://db.example.org:5432/app");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(environment));
    }

    @Test
    void prodRechazaCombinacionConDevYOrmUpdate() {
        MockEnvironment mixedProfiles = productionEnvironment();
        mixedProfiles.setActiveProfiles("prod", "dev");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(mixedProfiles));

        MockEnvironment updateSchema = productionEnvironment();
        updateSchema.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(updateSchema));

        MockEnvironment runningMigrations = productionEnvironment();
        runningMigrations.setProperty("spring.flyway.enabled", "true");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(runningMigrations));

        MockEnvironment trustingProxyHeaders = productionEnvironment();
        trustingProxyHeaders.setProperty("server.forward-headers-strategy", "framework");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(trustingProxyHeaders));
    }

    @Test
    void prodRechazaClaveJwtInsuficienteOCorsAbierto() {
        MockEnvironment shortKey = productionEnvironment();
        shortKey.setProperty("JWT_SECRET_BASE64", Base64.getEncoder().encodeToString(new byte[16]));
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(shortKey));

        MockEnvironment wildcardCors = productionEnvironment();
        wildcardCors.setProperty("app.cors.allowed-origins", "*");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(wildcardCors));
    }

    @Test
    void prodExigeStartTlsYEnlaceDeRecuperacionHttpsCuandoSeConfigura() {
        MockEnvironment secure = productionEnvironment();
        secure.setProperty("app.mail.reset-url", "https://app.example.org/reset-password");
        assertDoesNotThrow(() -> ProductionEnvironmentGuard.validateProductionEnvironment(secure));

        MockEnvironment insecureUrl = productionEnvironment();
        insecureUrl.setProperty("app.mail.reset-url", "http://app.example.org/reset-password");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(insecureUrl));

        MockEnvironment missingUrl = productionEnvironment();
        missingUrl.setProperty("spring.mail.host", "smtp.example.org");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(missingUrl));

        MockEnvironment optionalTls = productionEnvironment();
        optionalTls.setProperty("spring.mail.host", "smtp.example.org");
        optionalTls.setProperty("app.mail.reset-url", "https://app.example.org/reset");
        optionalTls.setProperty("spring.mail.properties.mail.smtp.starttls.required", "false");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(optionalTls));

        MockEnvironment disabledTls = productionEnvironment();
        disabledTls.setProperty("spring.mail.host", "smtp.example.org");
        disabledTls.setProperty("app.mail.reset-url", "https://app.example.org/reset");
        disabledTls.setProperty("spring.mail.properties.mail.smtp.starttls.enable", "false");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(disabledTls));

        MockEnvironment unverifiedServer = productionEnvironment();
        unverifiedServer.setProperty("spring.mail.host", "smtp.example.org");
        unverifiedServer.setProperty("app.mail.reset-url", "https://app.example.org/reset");
        unverifiedServer.setProperty("spring.mail.properties.mail.smtp.ssl.checkserveridentity", "false");
        assertThrows(IllegalStateException.class,
                () -> ProductionEnvironmentGuard.validateProductionEnvironment(unverifiedServer));
    }

    private static MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("DB_URL", "jdbc:postgresql://db.example.org:5432/app?sslmode=require");
        environment.setProperty("DB_USERNAME", "test-user");
        environment.setProperty("DB_PASSWORD", "test-only-placeholder");
        environment.setProperty("JWT_SECRET_BASE64", Base64.getEncoder().encodeToString(new byte[32]));
        environment.setProperty("JWT_TTL_SECONDS", "900");
        environment.setProperty("JWT_ISSUER", "test-issuer");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        environment.setProperty("spring.flyway.enabled", "false");
        return environment;
    }
}
