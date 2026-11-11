package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.Usuario;
import java.util.Optional;

public interface ObtenerUsuarioUseCase {
    Optional<Usuario> obtener(Long id);
}
