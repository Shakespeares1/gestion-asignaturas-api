package com.gestionacademica.infrastructure.persistence.mapper;

import com.gestionacademica.domain.model.Usuario;
import com.gestionacademica.infrastructure.persistence.entity.UsuarioEntity;

public final class UsuarioMapper {
    private UsuarioMapper() {}

    public static Usuario toDomain(UsuarioEntity entity) {
        return new Usuario(entity.getId(), entity.getNombreCompleto(), entity.getEmail(),
                entity.getPasswordHash(), entity.getRole(), entity.getAccountStatus(),
                entity.getFechaCreacion(), entity.getResetTokenHash(), entity.getResetTokenExpiry());
    }

    public static UsuarioEntity toEntity(Usuario usuario) {
        return new UsuarioEntity(usuario.getId(), usuario.getNombreCompleto(), usuario.getEmail(),
                usuario.getPasswordHash(), usuario.getRole(), usuario.getAccountStatus(),
                usuario.getFechaCreacion(), usuario.getResetTokenHash(), usuario.getResetTokenExpiry());
    }
}
