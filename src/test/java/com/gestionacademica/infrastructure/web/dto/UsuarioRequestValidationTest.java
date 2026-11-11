package com.gestionacademica.infrastructure.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class UsuarioRequestValidationTest {
    @Test
    void rechazaCamposRequeridosVaciosYEmailInvalido() {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombreCompleto(" ");
        request.setEmail("no-es-correo");
        request.setPassword("");

        Set<String> invalidFields = validar(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of("nombreCompleto", "email", "password"), invalidFields);
    }

    @Test
    void aceptaDatosRequeridosValidosSinRecibirCamposProtegidos() {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombreCompleto("Ada Lovelace");
        request.setEmail("ada@example.org");
        request.setPassword("sample-only-password");

        assertTrue(validar(request).isEmpty());
        Set<String> requestFields = java.util.Arrays.stream(CrearUsuarioRequest.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertFalse(requestFields.contains("passwordHash"));
        assertFalse(requestFields.contains("resetTokenHash"));
        assertFalse(requestFields.contains("role"));
        assertFalse(requestFields.contains("accountStatus"));
        assertFalse(requestFields.contains("fechaCreacion"));
    }

    @Test
    void aplicaPoliticaComunARegistroYRestablecimientoEnLaEntradaRest() {
        CrearUsuarioRequest registration = new CrearUsuarioRequest();
        registration.setNombreCompleto("Ada Lovelace");
        registration.setEmail("ada@example.org");
        registration.setPassword("short-pass");
        assertTrue(validar(registration).stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));

        registration.setPassword("            ");
        assertTrue(validar(registration).stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));

        registration.setPassword("válida-password-12");
        assertTrue(validar(registration).isEmpty());

        registration.setPassword("é".repeat(37));
        assertTrue(validar(registration).stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setToken("a-valid-reset-token");
        reset.setNewPassword("short-pass");
        assertTrue(validar(reset).stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword")));
        reset.setNewPassword("new-password");
        assertTrue(validar(reset).isEmpty());
    }

    @Test
    void politicaNoCambiaValidacionDeLoginParaUsuariosConContrasenasAnteriores() {
        LoginRequest login = new LoginRequest();
        login.setEmail("legacy@example.org");
        login.setPassword("old-pass");
        assertTrue(validar(login).isEmpty());
    }

    @Test
    void respuestaNoDeclaraCamposDeContrasenaNiRecuperacion() {
        Set<String> properties = java.util.Arrays.stream(UsuarioResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(Collectors.toSet());

        assertFalse(properties.contains("password"));
        assertFalse(properties.contains("passwordHash"));
        assertFalse(properties.contains("resetTokenHash"));
        assertFalse(properties.contains("resetTokenExpiry"));
    }

    @Test
    void dtoDeActualizacionNoAceptaCamposProtegidos() {
        Set<String> fields = java.util.Arrays.stream(ActualizarUsuarioRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("nombreCompleto", "email"), fields);
    }

    private static Set<jakarta.validation.ConstraintViolation<CrearUsuarioRequest>> validar(CrearUsuarioRequest request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(request);
        }
    }

    private static Set<jakarta.validation.ConstraintViolation<ResetPasswordRequest>> validar(ResetPasswordRequest request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(request);
        }
    }

    private static Set<jakarta.validation.ConstraintViolation<LoginRequest>> validar(LoginRequest request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(request);
        }
    }
}
