package com.gestionacademica.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.application.port.output.AsignaturaRepositoryPort;
import com.gestionacademica.domain.model.Asignatura;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsignaturaCrudServiceTest {
    private RepositorioEnMemoria repository;
    private AsignaturaCrudService service;

    @BeforeEach
    void setUp() {
        repository = new RepositorioEnMemoria();
        service = new AsignaturaCrudService(repository);
    }

    @Test
    void debeListarYBuscarAsignaturas() {
        Asignatura saved = repository.guardar(asignatura(null, "POO", 3));

        assertEquals(List.of(saved), service.listar());
        assertEquals(Optional.of(saved), service.obtener(saved.getId()));
        assertTrue(service.obtener(999L).isEmpty());
    }

    @Test
    void debeActualizarAsignaturaExistente() {
        Asignatura saved = repository.guardar(asignatura(null, "POO", 3));

        Asignatura updated = service.actualizar(saved.getId(), asignatura(null, "POO II", 4));

        assertEquals(saved.getId(), updated.getId());
        assertEquals("POO II", updated.getNombre());
        assertEquals(4, service.obtener(saved.getId()).orElseThrow().getNumeroCreditos());
    }

    @Test
    void debeRechazarActualizacionDeAsignaturaInexistente() {
        assertThrows(AsignaturaNoEncontradaException.class,
                () -> service.actualizar(999L, asignatura(null, "POO", 3)));
    }

    @Test
    void debeEliminarSoloUnaAsignaturaExistente() {
        Asignatura saved = repository.guardar(asignatura(null, "POO", 3));

        assertTrue(service.eliminar(saved.getId()));
        assertTrue(service.obtener(saved.getId()).isEmpty());
        assertFalse(service.eliminar(saved.getId()));
    }

    private static Asignatura asignatura(Long id, String nombre, int creditos) {
        return new Asignatura(id, nombre, "Asignatura de prueba", null, null, null,
                creditos, null, 2, null);
    }

    private static final class RepositorioEnMemoria implements AsignaturaRepositoryPort {
        private final Map<Long, Asignatura> items = new HashMap<>();
        private long nextId = 1;

        @Override
        public Asignatura guardar(Asignatura asignatura) {
            Long id = asignatura.getId() == null ? nextId++ : asignatura.getId();
            Asignatura stored = new Asignatura(id, asignatura.getNombre(), asignatura.getNombreCompleto(),
                    asignatura.getDescripcion(), asignatura.getAreaConocimiento(), asignatura.getCarrera(),
                    asignatura.getNumeroCreditos(), asignatura.getContenidoTematico(), asignatura.getSemestre(),
                    asignatura.getProfesor());
            items.put(id, stored);
            return stored;
        }

        @Override public List<Asignatura> listar() { return List.copyOf(items.values()); }
        @Override public Optional<Asignatura> buscarPorId(Long id) { return Optional.ofNullable(items.get(id)); }
        @Override public boolean existePorId(Long id) { return items.containsKey(id); }
        @Override public void eliminarPorId(Long id) { items.remove(id); }
    }
}
