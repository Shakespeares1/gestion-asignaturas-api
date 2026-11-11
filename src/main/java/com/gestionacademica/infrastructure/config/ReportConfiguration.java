package com.gestionacademica.infrastructure.config;

import com.gestionacademica.application.port.input.ReporteAsignaturasUseCase;
import com.gestionacademica.application.port.input.ReporteUsuariosUseCase;
import com.gestionacademica.application.port.output.AsignaturaReportPort;
import com.gestionacademica.application.port.output.UsuarioReportPort;
import com.gestionacademica.application.service.ReporteAsignaturasService;
import com.gestionacademica.application.service.ReporteUsuariosService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportConfiguration {
    @Bean
    ReporteAsignaturasUseCase reporteAsignaturasUseCase(AsignaturaReportPort reports) {
        return new ReporteAsignaturasService(reports);
    }

    @Bean
    ReporteUsuariosUseCase reporteUsuariosUseCase(UsuarioReportPort reports) {
        return new ReporteUsuariosService(reports);
    }
}
