package com.gestionacademica.infrastructure.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.domain.model.Asignatura;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AsignaturaRequestValidationTest {

    @Test
    void aceptaCadenasEnLosLimitesDeLasColumnas() {
        AsignaturaRequest request = new AsignaturaRequest(
                "N".repeat(Asignatura.MAX_NOMBRE),
                "C".repeat(Asignatura.MAX_NOMBRE_COMPLETO), null,
                "A".repeat(Asignatura.MAX_AREA_CONOCIMIENTO),
                "R".repeat(Asignatura.MAX_CARRERA), 1, null, 1,
                "P".repeat(Asignatura.MAX_PROFESOR));

        assertTrue(validar(request).isEmpty());
    }

    @Test
    void rechazaCadenasQueSuperanLasColumnas() {
        AsignaturaRequest request = new AsignaturaRequest(
                "N".repeat(Asignatura.MAX_NOMBRE + 1),
                "C".repeat(Asignatura.MAX_NOMBRE_COMPLETO + 1), null,
                "A".repeat(Asignatura.MAX_AREA_CONOCIMIENTO + 1),
                "R".repeat(Asignatura.MAX_CARRERA + 1), 1, null, 1,
                "P".repeat(Asignatura.MAX_PROFESOR + 1));

        Set<String> invalidFields = validar(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of("nombre", "nombreCompleto", "areaConocimiento", "carrera", "profesor"), invalidFields);
    }

    @Test
    void rechazaValoresRequeridosVaciosONoPositivos() {
        AsignaturaRequest request = new AsignaturaRequest(" ", "", null, null, null,
                0, null, 0, null);

        Set<String> invalidFields = validar(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(invalidFields.containsAll(Set.of("nombre", "nombreCompleto", "numeroCreditos", "semestre")));
    }

    private static Set<jakarta.validation.ConstraintViolation<AsignaturaRequest>> validar(AsignaturaRequest request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(request);
        }
    }
}
