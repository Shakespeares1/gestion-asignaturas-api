package com.gestionacademica.application.service;

import com.gestionacademica.application.port.input.CambiarAccesoUsuarioUseCase;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;

public class CambiarAccesoUsuarioService implements CambiarAccesoUsuarioUseCase {
    private final UsuarioRepositoryPort usuarios;

    public CambiarAccesoUsuarioService(UsuarioRepositoryPort usuarios) { this.usuarios = usuarios; }

    @Override
    public Usuario cambiar(Long id, RolUsuario role, EstadoCuenta estado) {
        Usuario current = usuarios.buscarPorId(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
        return usuarios.guardar(current.actualizarAcceso(role, estado));
    }
}
