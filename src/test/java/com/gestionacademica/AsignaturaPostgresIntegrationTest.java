package com.gestionacademica;

import static org.junit.jupiter.api.Assertions.*;

import com.gestionacademica.application.port.input.ActualizarAsignaturaUseCase;
import com.gestionacademica.application.port.input.CrearAsignaturaUseCase;
import com.gestionacademica.application.port.input.EliminarAsignaturaUseCase;
import com.gestionacademica.application.port.input.ObtenerAsignaturaUseCase;
import com.gestionacademica.domain.model.Asignatura;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class AsignaturaPostgresIntegrationTest {

    @Test
    @EnabledIf(value = "integrationDatabaseConfigured", disabledReason = "Define RUN_POSTGRES_INTEGRATION=true, SPRING_PROFILES_ACTIVE=postgres-integration y las variables DB_URL, DB_USERNAME y DB_PASSWORD para probar PostgreSQL")
    void debeCrearConsultarActualizarYEliminarAsignaturaEnPostgres() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GestionAsignaturasApiApplication.class)
                .web(WebApplicationType.NONE)
                .run()) {
            CrearAsignaturaUseCase crear = context.getBean(CrearAsignaturaUseCase.class);
            ObtenerAsignaturaUseCase obtener = context.getBean(ObtenerAsignaturaUseCase.class);
            ActualizarAsignaturaUseCase actualizar = context.getBean(ActualizarAsignaturaUseCase.class);
            EliminarAsignaturaUseCase eliminar = context.getBean(EliminarAsignaturaUseCase.class);

            String nombreUnico = "Integracion-" + UUID.randomUUID();
            Asignatura creada = crear.ejecutar(asignatura(null, nombreUnico, 3));
            assertNotNull(creada.getId());
            try {
                Asignatura consultada = obtener.obtener(creada.getId()).orElseThrow();
                assertEquals(nombreUnico, consultada.getNombre());
                assertEquals(3, consultada.getNumeroCreditos());

                Asignatura actualizada = actualizar.actualizar(creada.getId(), asignatura(null, nombreUnico, 4));
                assertEquals(4, actualizada.getNumeroCreditos());
                assertEquals(4, obtener.obtener(creada.getId()).orElseThrow().getNumeroCreditos());

                assertTrue(eliminar.eliminar(creada.getId()));
                assertTrue(obtener.obtener(creada.getId()).isEmpty());
            } finally {
                if (obtener.obtener(creada.getId()).isPresent()) {
                    eliminar.eliminar(creada.getId());
                }
            }
        }
    }

    static boolean integrationDatabaseConfigured() {
        return "true".equalsIgnoreCase(System.getenv("RUN_POSTGRES_INTEGRATION"))
                && "postgres-integration".equalsIgnoreCase(System.getenv("SPRING_PROFILES_ACTIVE"))
                && present("DB_URL") && present("DB_USERNAME") && present("DB_PASSWORD");
    }

    private static boolean present(String name) {
        return System.getenv(name) != null && !System.getenv(name).isBlank();
    }

    private static Asignatura asignatura(Long id, String nombre, int creditos) {
        return new Asignatura(id, nombre, "Asignatura de integración", "Registro temporal de prueba",
                "Ingeniería", "Ingeniería de Software", creditos, "Contenido temporal", 2, "Docente de prueba");
    }
}
