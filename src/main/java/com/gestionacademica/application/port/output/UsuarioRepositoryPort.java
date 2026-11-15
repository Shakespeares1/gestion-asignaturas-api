package com.gestionacademica.application.port.output;

import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {
    Usuario guardar(Usuario usuario);
    List<Usuario> listar();
    Optional<Usuario> buscarPorId(Long id);
    Optional<Usuario> buscarPorEmail(String emailNormalizado);
    Optional<Usuario> buscarPorUsername(String usernameNormalizado);
    boolean existePorRol(RolUsuario role);
    Optional<Usuario> buscarPorHashTokenRecuperacion(String tokenHash);
    boolean restablecerPasswordSiTokenVigente(String tokenHash, Instant ahora, String nuevoPasswordHash);
    void eliminarPorId(Long id);
}
