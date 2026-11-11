package com.gestionacademica.infrastructure.security;

import com.gestionacademica.application.port.output.PasswordResetEmailPort;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("!test")
public class JavaMailPasswordResetAdapter implements PasswordResetEmailPort {
    private final ObjectProvider<JavaMailSender> senderProvider;
    private final Environment environment;

    public JavaMailPasswordResetAdapter(ObjectProvider<JavaMailSender> senderProvider, Environment environment) {
        this.senderProvider = senderProvider;
        this.environment = environment;
    }

    @Override
    public boolean isAvailable() {
        boolean authenticationRequired = environment.getProperty("spring.mail.properties.mail.smtp.auth", Boolean.class, true);
        boolean startTlsEnabled = environment.getProperty(
                "spring.mail.properties.mail.smtp.starttls.enable", Boolean.class, true);
        boolean startTlsRequired = environment.getProperty(
                "spring.mail.properties.mail.smtp.starttls.required", Boolean.class, true);
        boolean verifyServerIdentity = environment.getProperty(
                "spring.mail.properties.mail.smtp.ssl.checkserveridentity", Boolean.class, true);
        return senderProvider.getIfAvailable() != null
                && startTlsEnabled && startTlsRequired && verifyServerIdentity
                && configured("spring.mail.host")
                && configured("app.mail.from")
                && configured("app.mail.reset-url")
                && (!authenticationRequired || (configured("spring.mail.username") && configured("spring.mail.password")));
    }

    @Override
    public void sendResetLink(String recipient, String rawToken, Instant expiresAt) {
        if (!isAvailable()) throw new IllegalStateException("Correo no configurado");
        String link = environment.getRequiredProperty("app.mail.reset-url")
                + (environment.getRequiredProperty("app.mail.reset-url").contains("?") ? "&" : "?")
                + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(environment.getRequiredProperty("app.mail.from"));
        message.setTo(recipient);
        message.setSubject("Recuperación de contraseña");
        message.setText("Solicitaste restablecer tu contraseña. Abre este enlace antes de " + expiresAt + ":\n" + link);
        senderProvider.getObject().send(message);
    }

    private boolean configured(String key) {
        String value = environment.getProperty(key);
        return value != null && !value.isBlank();
    }
}
