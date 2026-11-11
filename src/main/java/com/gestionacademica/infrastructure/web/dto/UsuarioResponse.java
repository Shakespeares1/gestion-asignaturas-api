package com.gestionacademica.infrastructure.web.dto;

import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.Instant;

public record UsuarioResponse(Long id, String nombreCompleto, String email, RolUsuario role,
        EstadoCuenta accountStatus, Instant fechaCreacion) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombreCompleto(), usuario.getEmail(),
                usuario.getRole(), usuario.getAccountStatus(), usuario.getFechaCreacion());
    }
}
