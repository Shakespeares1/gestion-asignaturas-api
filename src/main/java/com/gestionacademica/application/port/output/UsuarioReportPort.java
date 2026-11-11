package com.gestionacademica.application.port.output;

import com.gestionacademica.application.dto.ReportePage;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.Instant;

public interface UsuarioReportPort {
    ReportePage<Usuario> buscarPorRolEstado(RolUsuario role, EstadoCuenta accountStatus, int page, int size);
    ReportePage<Usuario> buscarPorFechaCreacion(Instant desdeInclusive, Instant hastaExclusivo, int page, int size);
}
