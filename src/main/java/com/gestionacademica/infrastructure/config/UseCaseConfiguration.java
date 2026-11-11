package com.gestionacademica.infrastructure.config;

import com.gestionacademica.application.port.input.CrearAsignaturaUseCase;
import com.gestionacademica.application.port.output.AsignaturaRepositoryPort;
import com.gestionacademica.application.service.AsignaturaCrudService;
import com.gestionacademica.application.service.CrearAsignaturaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {
    @Bean CrearAsignaturaUseCase crearAsignaturaUseCase(AsignaturaRepositoryPort repo) { return new CrearAsignaturaService(repo); }
    @Bean AsignaturaCrudService asignaturaCrudService(AsignaturaRepositoryPort repo) { return new AsignaturaCrudService(repo); }
}
