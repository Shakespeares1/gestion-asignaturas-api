package com.gestionacademica.application.service;

import com.gestionacademica.application.dto.ReportePage;
import com.gestionacademica.application.port.input.ReporteAsignaturasUseCase;
import com.gestionacademica.application.port.output.AsignaturaReportPort;
import com.gestionacademica.domain.model.Asignatura;

public class ReporteAsignaturasService implements ReporteAsignaturasUseCase {
    private final AsignaturaReportPort reports;

    public ReporteAsignaturasService(AsignaturaReportPort reports) { this.reports = reports; }

    @Override
    public ReportePage<Asignatura> porCarreraSemestreArea(String carrera, Integer semestre,
            String areaConocimiento, int page, int size) {
        validatePage(page, size);
        if (semestre != null && semestre < 1) throw new RangoReporteInvalidoException("El semestre debe ser positivo");
        return reports.buscarPorCarreraSemestreArea(clean(carrera), semestre, clean(areaConocimiento), page, size);
    }

    @Override
    public ReportePage<Asignatura> porCreditosProfesorNombre(Integer min, Integer max,
            String profesor, String nombre, int page, int size) {
        validatePage(page, size);
        if (min != null && min < 1 || max != null && max < 1) {
            throw new RangoReporteInvalidoException("Los créditos deben ser positivos");
        }
        if (min != null && max != null && min > max) {
            throw new RangoReporteInvalidoException("El crédito mínimo no puede superar el máximo");
        }
        return reports.buscarPorCreditosProfesorNombre(min, max, clean(profesor), clean(nombre), page, size);
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new RangoReporteInvalidoException("La página debe ser >= 0 y el tamaño entre 1 y 100");
    }

    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
