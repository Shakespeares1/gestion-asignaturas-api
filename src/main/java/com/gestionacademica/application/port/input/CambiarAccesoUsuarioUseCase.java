package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;

public interface CambiarAccesoUsuarioUseCase {
    Usuario cambiar(Long id, RolUsuario role, EstadoCuenta estado);
}
