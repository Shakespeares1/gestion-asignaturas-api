package com.gestionacademica.application.port.input;

import com.gestionacademica.application.dto.ReportePage;
import com.gestionacademica.domain.model.Asignatura;

public interface ReporteAsignaturasUseCase {
    ReportePage<Asignatura> porCarreraSemestreArea(String carrera, Integer semestre,
            String areaConocimiento, int page, int size);

    ReportePage<Asignatura> porCreditosProfesorNombre(Integer creditosMin, Integer creditosMax,
            String profesor, String nombre, int page, int size);
}
