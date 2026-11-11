package com.gestionacademica.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mail.javamail.JavaMailSender;

class JavaMailPasswordResetAdapterTest {
    @Test
    void soloConsideraDisponibleElCorreoConStartTlsHabilitadoYObligatorio() {
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(JavaMailSender.class));
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.mail.host", "smtp.example.org")
                .withProperty("app.mail.from", "no-reply@example.org")
                .withProperty("app.mail.reset-url", "https://app.example.org/reset")
                .withProperty("spring.mail.username", "test-user")
                .withProperty("spring.mail.password", "test-only-placeholder");
        JavaMailPasswordResetAdapter adapter = new JavaMailPasswordResetAdapter(provider, environment);

        assertTrue(adapter.isAvailable());
        environment.setProperty("spring.mail.properties.mail.smtp.starttls.required", "false");
        assertFalse(adapter.isAvailable());
        environment.setProperty("spring.mail.properties.mail.smtp.starttls.required", "true");
        environment.setProperty("spring.mail.properties.mail.smtp.starttls.enable", "false");
        assertFalse(adapter.isAvailable());
        environment.setProperty("spring.mail.properties.mail.smtp.starttls.enable", "true");
        environment.setProperty("spring.mail.properties.mail.smtp.ssl.checkserveridentity", "false");
        assertFalse(adapter.isAvailable());
    }
}
