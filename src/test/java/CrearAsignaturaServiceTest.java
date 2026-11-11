
package com.gestionacademica.application.service;

import org.junit.jupiter.api.Test;

import com.gestionacademica.application.port.output.AsignaturaRepositoryPort;
import com.gestionacademica.domain.model.Asignatura;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CrearAsignaturaServiceTest {

    @Test
    void debeCrearAsignaturaCorrectamente() {

        AsignaturaRepositoryPort repositorioFalso = repositorioFalso();

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

        AsignaturaRepositoryPort repositorioFalso = repositorioFalso();

        CrearAsignaturaService servicio =
                new CrearAsignaturaService(repositorioFalso);

        assertThrows(
                NullPointerException.class,
                () -> servicio.ejecutar(null)
        );
    }

    private AsignaturaRepositoryPort repositorioFalso() {
        return new AsignaturaRepositoryPort() {
            @Override public Asignatura guardar(Asignatura asignatura) { return asignatura; }
            @Override public List<Asignatura> listar() { return List.of(); }
            @Override public Optional<Asignatura> buscarPorId(Long id) { return Optional.empty(); }
            @Override public boolean existePorId(Long id) { return false; }
            @Override public void eliminarPorId(Long id) {}
        };
    }
}
