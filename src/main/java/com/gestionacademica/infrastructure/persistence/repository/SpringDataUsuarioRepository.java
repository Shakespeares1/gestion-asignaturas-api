package com.gestionacademica.infrastructure.persistence.repository;

import com.gestionacademica.infrastructure.persistence.entity.UsuarioEntity;
import java.util.Optional;
import java.time.Instant;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioEntity, Long>,
        JpaSpecificationExecutor<UsuarioEntity> {
    Optional<UsuarioEntity> findByEmail(String email);
    boolean existsByRole(RolUsuario role);
    Optional<UsuarioEntity> findByResetTokenHash(String resetTokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UsuarioEntity u set u.passwordHash = :passwordHash, u.resetTokenHash = null, " +
            "u.resetTokenExpiry = null where u.resetTokenHash = :tokenHash " +
            "and u.resetTokenExpiry > :now and u.accountStatus = :active")
    int updatePasswordForValidResetToken(@Param("tokenHash") String tokenHash,
            @Param("now") Instant now, @Param("passwordHash") String passwordHash,
            @Param("active") EstadoCuenta active);
}
