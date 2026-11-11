package com.gestionacademica.application.port.input;

import com.gestionacademica.domain.model.Usuario;
import java.util.List;

public interface ListarUsuariosUseCase {
    List<Usuario> listar();
}
