package com.gestionacademica.application.port.output;

import com.gestionacademica.domain.model.Usuario;
import java.util.List;
import java.util.Optional;
import com.gestionacademica.domain.model.RolUsuario;

public interface UsuarioRepositoryPort {
    Usuario guardar(Usuario usuario);
    List<Usuario> listar();
    Optional<Usuario> buscarPorId(Long id);
    Optional<Usuario> buscarPorEmail(String emailNormalizado);
    boolean existePorRol(RolUsuario role);
    Optional<Usuario> buscarPorHashTokenRecuperacion(String tokenHash);
    boolean restablecerPasswordSiTokenVigente(String tokenHash, java.time.Instant ahora, String nuevoPasswordHash);
    void eliminarPorId(Long id);
}
