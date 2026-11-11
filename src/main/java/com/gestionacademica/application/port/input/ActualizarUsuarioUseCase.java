package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.Usuario;

public interface ActualizarUsuarioUseCase {
    Usuario actualizar(Long id, String nombreCompleto, String email);
}
