package com.gestionacademica.application.service;

import com.gestionacademica.application.dto.ReportePage;
import com.gestionacademica.application.port.input.ReporteUsuariosUseCase;
import com.gestionacademica.application.port.output.UsuarioReportPort;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class ReporteUsuariosService implements ReporteUsuariosUseCase {
    private final UsuarioReportPort reports;

    public ReporteUsuariosService(UsuarioReportPort reports) { this.reports = reports; }

    @Override
    public ReportePage<Usuario> porRolEstado(RolUsuario role, EstadoCuenta status, int page, int size) {
        validatePage(page, size);
        return reports.buscarPorRolEstado(role, status, page, size);
    }

    @Override
    public ReportePage<Usuario> porFechaCreacion(LocalDate desde, LocalDate hasta, int page, int size) {
        validatePage(page, size);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new RangoReporteInvalidoException("La fecha inicial no puede ser posterior a la fecha final");
        }
        try {
            var fromInclusive = desde == null ? null : desde.atStartOfDay(ZoneOffset.UTC).toInstant();
            var toExclusive = hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return reports.buscarPorFechaCreacion(fromInclusive, toExclusive, page, size);
        } catch (java.time.DateTimeException | ArithmeticException exception) {
            throw new RangoReporteInvalidoException("El rango de fechas no es válido");
        }
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new RangoReporteInvalidoException("La página debe ser >= 0 y el tamaño entre 1 y 100");
    }
}
