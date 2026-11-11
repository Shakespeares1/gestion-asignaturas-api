package com.gestionacademica.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.application.port.input.ReporteAsignaturasUseCase;
import com.gestionacademica.application.port.input.ReporteUsuariosUseCase;
import com.gestionacademica.application.port.output.AsignaturaRepositoryPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.application.service.RangoReporteInvalidoException;
import com.gestionacademica.domain.model.Asignatura;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import com.gestionacademica.infrastructure.security.TestSecurityBeans;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class ReportesPersistenceTest {
    @Autowired private AsignaturaRepositoryPort asignaturas;
    @Autowired private UsuarioRepositoryPort usuarios;
    @Autowired private ReporteAsignaturasUseCase assignmentReports;
    @Autowired private ReporteUsuariosUseCase userReports;

    @Test
    @Transactional
    void reporteAsignaturasFiltraPorCarreraSemestreAreaIndividualYCombinado() {
        saveAssignment("Bases de datos", "Software", 3, "Ciencias de datos", 4, "Ana Pérez");
        saveAssignment("Algoritmos", "Software", 4, "Ingeniería", 3, "Luis Díaz");
        saveAssignment("Redes", "Telecomunicaciones", 3, "Redes", 5, "Ana Pérez");

        assertEquals(2, assignmentReports.porCarreraSemestreArea("SOFTWARE", null, null, 0, 10).totalElements());
        assertEquals(2, assignmentReports.porCarreraSemestreArea(null, 3, null, 0, 10).totalElements());
        assertEquals(1, assignmentReports.porCarreraSemestreArea(null, null, "ciencias", 0, 10).totalElements());
        var combined = assignmentReports.porCarreraSemestreArea("Software", 3, "Ciencias", 0, 10);
        assertEquals(1, combined.totalElements());
        assertEquals("Bases de datos", combined.items().getFirst().getNombre());
        assertEquals(0, assignmentReports.porCarreraSemestreArea("Desconocida", null, null, 0, 10).totalElements());
    }

    @Test
    @Transactional
    void reporteCreditosProfesorNombreUsaRangoInclusivoYFiltrosCombinados() {
        saveAssignment("Bases de datos", "Software", 3, "Ciencias", 4, "Ana Pérez");
        saveAssignment("Algoritmos", "Software", 4, "Ingeniería", 3, "Luis Díaz");
        saveAssignment("Redes", "Telecomunicaciones", 3, "Redes", 5, "Ana Pérez");

        var result = assignmentReports.porCreditosProfesorNombre(4, 5, "ana", "base", 0, 10);
        assertEquals(1, result.totalElements());
        assertEquals(0, assignmentReports.porCreditosProfesorNombre(10, 12, null, null, 0, 10).totalElements());
        assertEquals(1, assignmentReports.porCreditosProfesorNombre(null, 3, null, "algo", 0, 10).totalElements());
        assertThrows(RangoReporteInvalidoException.class,
                () -> assignmentReports.porCreditosProfesorNombre(5, 2, null, null, 0, 10));
    }

    @Test
    @Transactional
    void reporteUsuariosCombinaRolEstadoYNoDevuelveRegistrosFueraDelFiltro() {
        saveUser("report-admin-active@example.org", RolUsuario.ADMIN, EstadoCuenta.ACTIVO, "2026-02-01T12:00:00Z");
        saveUser("report-admin-inactive@example.org", RolUsuario.ADMIN, EstadoCuenta.INACTIVO, "2026-02-02T12:00:00Z");
        saveUser("report-user-inactive@example.org", RolUsuario.USER, EstadoCuenta.INACTIVO, "2026-02-03T12:00:00Z");

        assertEquals(2, userReports.porRolEstado(RolUsuario.ADMIN, null, 0, 10).totalElements());
        assertEquals(2, userReports.porRolEstado(null, EstadoCuenta.INACTIVO, 0, 10).totalElements());
        var filtered = userReports.porRolEstado(RolUsuario.ADMIN, EstadoCuenta.INACTIVO, 0, 10);
        assertEquals(1, filtered.totalElements());
        assertEquals("report-admin-inactive@example.org", filtered.items().getFirst().getEmail());
        assertEquals(0, userReports.porRolEstado(RolUsuario.USER, EstadoCuenta.ACTIVO, 0, 10).totalElements());
    }

    @Test
    @Transactional
    void reporteFechasIncluyeAmbosDiasYRechazaRangoInvertido() {
        saveUser("date-before@example.org", RolUsuario.USER, EstadoCuenta.ACTIVO, "2026-02-01T23:59:59Z");
        saveUser("date-start@example.org", RolUsuario.USER, EstadoCuenta.ACTIVO, "2026-02-02T00:00:00Z");
        saveUser("date-end@example.org", RolUsuario.USER, EstadoCuenta.ACTIVO, "2026-02-02T23:59:59Z");
        saveUser("date-after@example.org", RolUsuario.USER, EstadoCuenta.ACTIVO, "2026-02-03T00:00:00Z");

        var result = userReports.porFechaCreacion(LocalDate.parse("2026-02-02"), LocalDate.parse("2026-02-02"), 0, 10);
        assertEquals(2, result.totalElements());
        assertThrows(RangoReporteInvalidoException.class,
                () -> userReports.porFechaCreacion(LocalDate.parse("2026-02-03"), LocalDate.parse("2026-02-02"), 0, 10));
    }

    @Test
    @Transactional
    void paginasLimitaLosResultadosYExponeConteoTotal() {
        saveAssignment("Page A", "Software", 1, "Matemáticas", 1, "Teacher");
        saveAssignment("Page B", "Software", 1, "Matemáticas", 1, "Teacher");
        var page = assignmentReports.porCarreraSemestreArea("Software", null, null, 0, 1);
        assertEquals(1, page.items().size());
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
    }

    private void saveAssignment(String name, String carrera, int semester, String area, int credits, String professor) {
        asignaturas.guardar(new Asignatura(null, name, name, null, area, carrera, credits, null, semester, professor));
    }

    private void saveUser(String email, RolUsuario role, EstadoCuenta status, String created) {
        usuarios.guardar(new Usuario(null, "Report User", email, "{bcrypt}test-hash", role, status,
                Instant.parse(created), null, null));
    }
}
