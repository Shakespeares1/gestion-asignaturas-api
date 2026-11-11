package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.Usuario;

public interface CrearUsuarioUseCase {
    Usuario crear(String nombreCompleto, String email, String password);
}
