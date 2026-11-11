package com.gestionacademica.infrastructure.config;

import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;
import java.net.URI;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionEnvironmentGuard {
    private static final Pattern REQUIRED_SSL = Pattern.compile("(?i)(?:[?&])sslmode=require(?:&|$)");

    @Bean
    static BeanFactoryPostProcessor validateProductionEnvironment(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        if (Arrays.stream(profiles).anyMatch(profile -> profile.equals("dev") || profile.equals("test")
                || profile.equals("postgres-integration"))) {
            throw new IllegalStateException("El perfil prod no se puede combinar con perfiles dev/test/integración");
        }
        String url = required(environment, "DB_URL");
        if (!url.startsWith("jdbc:postgresql:") || !REQUIRED_SSL.matcher(url).find()) {
            throw new IllegalStateException("DB_URL debe ser PostgreSQL con sslmode=require");
        }
        required(environment, "DB_USERNAME");
        required(environment, "DB_PASSWORD");
        if (!"validate".equals(environment.getProperty("spring.jpa.hibernate.ddl-auto"))) {
            throw new IllegalStateException("Producción debe usar spring.jpa.hibernate.ddl-auto=validate");
        }
        if (!"none".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy", "none"))) {
            throw new IllegalStateException("Producción debe ignorar encabezados proxy manipulables; usar CF-Connecting-IP para rate limit en Render");
        }
        String jwtSecret = required(environment, "JWT_SECRET_BASE64");
        try {
            if (Base64.getDecoder().decode(jwtSecret).length < 32) {
                throw new IllegalStateException("JWT_SECRET_BASE64 debe contener al menos 32 bytes");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 debe ser Base64 válido", exception);
        }
        String ttl = environment.getProperty("JWT_TTL_SECONDS", "900");
        try {
            long seconds = Long.parseLong(ttl);
            if (seconds < 60 || seconds > 86_400) throw new IllegalStateException("JWT_TTL_SECONDS fuera de rango");
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("JWT_TTL_SECONDS debe ser numérico", exception);
        }
        if (environment.getProperty("JWT_ISSUER", "gestion-academica").isBlank()) {
            throw new IllegalStateException("JWT_ISSUER no puede estar vacío");
        }
        if (environment.getProperty("app.cors.allowed-origins", "").contains("*")) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS debe contener orígenes explícitos, no comodines");
        }
        boolean smtpConfigured = !environment.getProperty("spring.mail.host", "").isBlank();
        boolean startTlsEnabled = environment.getProperty(
                "spring.mail.properties.mail.smtp.starttls.enable", Boolean.class, true);
        boolean startTlsRequired = environment.getProperty(
                "spring.mail.properties.mail.smtp.starttls.required", Boolean.class, true);
        boolean verifySmtpIdentity = environment.getProperty(
                "spring.mail.properties.mail.smtp.ssl.checkserveridentity", Boolean.class, true);
        if (smtpConfigured && (!startTlsEnabled || !startTlsRequired || !verifySmtpIdentity)) {
            throw new IllegalStateException("Producción debe exigir STARTTLS y verificar la identidad del servidor SMTP");
        }
        String resetUrl = environment.getProperty("app.mail.reset-url", "").strip();
        if (smtpConfigured && resetUrl.isEmpty()) {
            throw new IllegalStateException("APP_MAIL_RESET_URL es obligatorio cuando SMTP está configurado en producción");
        }
        if (!resetUrl.isEmpty()) {
            try {
                URI uri = URI.create(resetUrl);
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                        || uri.getUserInfo() != null || uri.getFragment() != null) {
                    throw new IllegalStateException("APP_MAIL_RESET_URL debe ser una URL HTTPS válida en producción");
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("APP_MAIL_RESET_URL debe ser una URL HTTPS válida en producción");
            }
        }
        return beanFactory -> {};
    }

    private static String required(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Falta variable de entorno requerida: " + key);
        return value;
    }
}
