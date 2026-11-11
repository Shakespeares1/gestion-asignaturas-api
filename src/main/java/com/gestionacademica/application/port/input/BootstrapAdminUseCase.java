package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.Usuario;

public interface BootstrapAdminUseCase {
    Usuario crearPrimerAdministrador(String nombreCompleto, String email, String password);
}
