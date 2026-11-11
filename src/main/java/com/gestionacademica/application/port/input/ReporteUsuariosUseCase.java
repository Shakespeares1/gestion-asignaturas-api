package com.gestionacademica.application.port.input;

import com.gestionacademica.application.dto.ReportePage;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.LocalDate;

public interface ReporteUsuariosUseCase {
    ReportePage<Usuario> porRolEstado(RolUsuario role, EstadoCuenta accountStatus, int page, int size);
    ReportePage<Usuario> porFechaCreacion(LocalDate fechaDesde, LocalDate fechaHasta, int page, int size);
}
