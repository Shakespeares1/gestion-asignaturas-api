
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

    @Test
    void debeAceptarCamposEnLosLimitesDefinidos() {
        Asignatura asignatura = crearConCampos(
                "N".repeat(Asignatura.MAX_NOMBRE),
                "C".repeat(Asignatura.MAX_NOMBRE_COMPLETO),
                "A".repeat(Asignatura.MAX_AREA_CONOCIMIENTO),
                "R".repeat(Asignatura.MAX_CARRERA),
                "P".repeat(Asignatura.MAX_PROFESOR));

        assertEquals(Asignatura.MAX_NOMBRE, asignatura.getNombre().length());
    }

    @Test
    void debeRechazarCamposQueSuperanLosLimitesDefinidos() {
        assertThrows(IllegalArgumentException.class, () -> crearConCampos(
                "N".repeat(Asignatura.MAX_NOMBRE + 1), "Completo", "Área", "Carrera", "Profesor"));
        assertThrows(IllegalArgumentException.class, () -> crearConCampos(
                "Nombre", "C".repeat(Asignatura.MAX_NOMBRE_COMPLETO + 1), "Área", "Carrera", "Profesor"));
        assertThrows(IllegalArgumentException.class, () -> crearConCampos(
                "Nombre", "Completo", "A".repeat(Asignatura.MAX_AREA_CONOCIMIENTO + 1), "Carrera", "Profesor"));
        assertThrows(IllegalArgumentException.class, () -> crearConCampos(
                "Nombre", "Completo", "Área", "R".repeat(Asignatura.MAX_CARRERA + 1), "Profesor"));
        assertThrows(IllegalArgumentException.class, () -> crearConCampos(
                "Nombre", "Completo", "Área", "Carrera", "P".repeat(Asignatura.MAX_PROFESOR + 1)));
    }

    private Asignatura crearConCampos(String nombre, String nombreCompleto, String area,
            String carrera, String profesor) {
        return new Asignatura(null, nombre, nombreCompleto, null, area, carrera, 1, null, 1, profesor);
    }
}
