
package com.gestionacademica.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AsignaturaTest {

    private Asignatura crearAsignatura(
            String nombre, int creditos, int semestre) {

        return new Asignatura(
                null,
                nombre,
                "Programación Orientada a Objetos",
                "Fundamentos de POO",
                "Programación",
                "Ingeniería de Software",
                creditos,
                "Clases, objetos y herencia",
                semestre,
                "Docente de prueba"
        );
    }

    @Test
    void debeCrearAsignaturaValida() {
        Asignatura asignatura =
                crearAsignatura("POO", 3, 2);

        assertEquals("POO", asignatura.getNombre());
        assertEquals(3, asignatura.getNumeroCreditos());
        assertEquals(2, asignatura.getSemestre());
    }

    @Test
    void debeRechazarNombreVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> crearAsignatura("", 3, 2)
        );
    }

    @Test
    void debeRechazarCreditosNegativos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> crearAsignatura("POO", -1, 2)
        );
    }

    @Test
    void debeRechazarSemestreCero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> crearAsignatura("POO", 3, 0)
        );
    }
}
