package com.gestionacademica.infrastructure.config;

import com.gestionacademica.application.port.input.BootstrapAdminUseCase;
import com.gestionacademica.application.port.output.AdminBootstrapLockPort;
import com.gestionacademica.application.port.output.PasswordHasherPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.application.service.BootstrapAdminService;
import com.gestionacademica.domain.model.Usuario;
import java.io.Console;
import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("admin-bootstrap")
public class AdminBootstrapConfiguration {
    @Bean
    static BeanFactoryPostProcessor validateBootstrapExecution(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        if (Arrays.stream(profiles).anyMatch(profile -> profile.equals("dev") || profile.equals("test")
                || profile.equals("postgres-integration"))) {
            throw new IllegalStateException("admin-bootstrap no se puede combinar con perfiles dev/test/integración");
        }
        if (!"none".equalsIgnoreCase(environment.getProperty("spring.main.web-application-type", ""))) {
            throw new IllegalStateException("admin-bootstrap requiere spring.main.web-application-type=none");
        }
        if (environment.getProperty("spring.flyway.enabled", Boolean.class, true)) {
            throw new IllegalStateException("admin-bootstrap requiere Flyway deshabilitado explícitamente");
        }
        if (!"validate".equals(environment.getProperty("spring.jpa.hibernate.ddl-auto"))) {
            throw new IllegalStateException("admin-bootstrap requiere Hibernate ddl-auto=validate");
        }
        if (!"never".equalsIgnoreCase(environment.getProperty("spring.sql.init.mode", ""))) {
            throw new IllegalStateException("admin-bootstrap requiere spring.sql.init.mode=never");
        }
        String databaseUrl = required(environment, "DB_URL");
        if (!databaseUrl.startsWith("jdbc:postgresql:")
                || !databaseUrl.matches("(?i).*([?&])sslmode=require(&.*)?$")) {
            throw new IllegalStateException("admin-bootstrap requiere PostgreSQL con sslmode=require");
        }
        required(environment, "DB_USERNAME");
        required(environment, "DB_PASSWORD");
        return beanFactory -> {};
    }

    private static String required(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Falta variable de entorno requerida: " + key);
        return value;
    }

    @Bean
    BootstrapAdminService bootstrapAdminService(UsuarioRepositoryPort usuarios,
            PasswordHasherPort passwordHasher, AdminBootstrapLockPort lock, java.time.Clock clock) {
        return new BootstrapAdminService(usuarios, passwordHasher, lock, clock);
    }

    @Bean
    ApplicationRunner initialAdminBootstrap(BootstrapAdminUseCase bootstrap,
            TransactionTemplate transactionTemplate) {
        return new InitialAdminBootstrapRunner(bootstrap, transactionTemplate);
    }

    static final class InitialAdminBootstrapRunner implements ApplicationRunner {
        private final BootstrapAdminUseCase bootstrap;
        private final TransactionTemplate transactionTemplate;

        InitialAdminBootstrapRunner(BootstrapAdminUseCase bootstrap,
                TransactionTemplate transactionTemplate) {
            this.bootstrap = bootstrap;
            this.transactionTemplate = transactionTemplate;
        }

        @Override
        public void run(ApplicationArguments args) {
            String name = requiredEnvironment("BOOTSTRAP_ADMIN_NAME");
            String email = requiredEnvironment("BOOTSTRAP_ADMIN_EMAIL");
            Console console = System.console();
            if (console == null) {
                throw new IllegalStateException("Se requiere una consola interactiva para capturar la contraseña sin mostrarla");
            }

            char[] password = console.readPassword("Contraseña inicial del administrador: ");
            if (password == null || password.length == 0) {
                if (password != null) Arrays.fill(password, '\0');
                throw new IllegalStateException("La contraseña no puede estar vacía");
            }
            try {
                String passwordValue = new String(password);
                Usuario created = transactionTemplate.execute(status ->
                        bootstrap.crearPrimerAdministrador(name, email, passwordValue));
                if (created == null) throw new IllegalStateException("No se pudo crear el administrador inicial");
                console.printf("Administrador inicial creado. El proceso finalizará ahora.%n");
            } finally {
                Arrays.fill(password, '\0');
            }
        }

        private static String requiredEnvironment(String key) {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Debe configurarse la variable de entorno " + key);
            }
            return value;
        }
    }
}
