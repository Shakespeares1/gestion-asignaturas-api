package com.gestionacademica.application.port.output;

import com.gestionacademica.application.dto.ReportePage;
import com.gestionacademica.domain.model.Asignatura;

public interface AsignaturaReportPort {
    ReportePage<Asignatura> buscarPorCarreraSemestreArea(String carrera, Integer semestre,
            String areaConocimiento, int page, int size);

    ReportePage<Asignatura> buscarPorCreditosProfesorNombre(Integer creditosMin, Integer creditosMax,
            String profesor, String nombre, int page, int size);
}
