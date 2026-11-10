
package com.gestionacademica.application.service;

import org.junit.jupiter.api.Test;

import com.gestionacademica.application.port.output.AsignaturaRepositoryPort;
import com.gestionacademica.domain.model.Asignatura;

import static org.junit.jupiter.api.Assertions.*;

class CrearAsignaturaServiceTest {

    @Test
    void debeCrearAsignaturaCorrectamente() {

        AsignaturaRepositoryPort repositorioFalso =
                asignatura -> asignatura;

        CrearAsignaturaService servicio =
                new CrearAsignaturaService(repositorioFalso);

        Asignatura asignatura = new Asignatura(
                null,
                "POO",
                "Programación Orientada a Objetos",
                "Fundamentos de POO",
                "Programación",
                "Ingeniería de Software",
                3,
                "Clases y objetos",
                2,
                "Docente de prueba"
        );

        Asignatura resultado = servicio.ejecutar(asignatura);

        assertNotNull(resultado);
        assertEquals("POO", resultado.getNombre());
        assertEquals(3, resultado.getNumeroCreditos());
    }

    @Test
    void debeRechazarAsignaturaNula() {

        AsignaturaRepositoryPort repositorioFalso =
                asignatura -> asignatura;

        CrearAsignaturaService servicio =
                new CrearAsignaturaService(repositorioFalso);

        assertThrows(
                NullPointerException.class,
                () -> servicio.ejecutar(null)
        );
    }
}
