package com.gestionacademica.infrastructure.persistence.entity;

import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuarios",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email"),
        indexes = {
                @Index(name = "idx_usuarios_user_role_account_status", columnList = "user_role, account_status"),
                @Index(name = "idx_usuarios_fecha_creacion", columnList = "fecha_creacion")
        })
@org.hibernate.annotations.Check(name = "ck_usuarios_role_valido", constraints = "user_role in ('ADMIN', 'USER')")
@org.hibernate.annotations.Check(name = "ck_usuarios_account_status_valido", constraints = "account_status in ('ACTIVO', 'INACTIVO')")
@org.hibernate.annotations.Check(name = "ck_usuarios_reset_token_pair", constraints = "(reset_token_hash is null and reset_token_expiry is null) or (reset_token_hash is not null and reset_token_expiry is not null)")
public class UsuarioEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "nombre_completo", nullable = false, columnDefinition = "text")
    private String nombreCompleto;
    @Column(nullable = false, length = Usuario.MAX_EMAIL)
    private String email;
    @Column(name = "password_hash", nullable = false, length = Usuario.MAX_PASSWORD_HASH)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 16)
    private RolUsuario role;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 16)
    private EstadoCuenta accountStatus;
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
    @Column(name = "reset_token_hash", length = Usuario.MAX_RESET_TOKEN_HASH)
    private String resetTokenHash;
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "reset_token_expiry")
    private Instant resetTokenExpiry;

    protected UsuarioEntity() {}

    public UsuarioEntity(Long id, String nombreCompleto, String email, String passwordHash,
            RolUsuario role, EstadoCuenta accountStatus, Instant fechaCreacion,
            String resetTokenHash, Instant resetTokenExpiry) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.accountStatus = accountStatus;
        this.fechaCreacion = fechaCreacion;
        this.resetTokenHash = resetTokenHash;
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public Long getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public RolUsuario getRole() { return role; }
    public EstadoCuenta getAccountStatus() { return accountStatus; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public String getResetTokenHash() { return resetTokenHash; }
    public Instant getResetTokenExpiry() { return resetTokenExpiry; }
}
