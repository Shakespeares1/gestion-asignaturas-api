package com.gestionacademica.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class Usuario {
    public static final int MAX_EMAIL = 254;
    public static final int MAX_PASSWORD_HASH = 255;
    public static final int MAX_RESET_TOKEN_HASH = 255;

    private final Long id;
    private final String nombreCompleto;
    private final String email;
    private final String passwordHash;
    private final RolUsuario role;
    private final EstadoCuenta accountStatus;
    private final Instant fechaCreacion;
    private final String resetTokenHash;
    private final Instant resetTokenExpiry;

    public Usuario(Long id, String nombreCompleto, String email, String passwordHash,
            RolUsuario role, EstadoCuenta accountStatus, Instant fechaCreacion,
            String resetTokenHash, Instant resetTokenExpiry) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }
        String normalizedEmail = normalizarEmail(email);
        validarMaximo(passwordHash, MAX_PASSWORD_HASH, "El hash de contraseña", false);
        if (role == null) throw new IllegalArgumentException("El rol es obligatorio");
        if (accountStatus == null) throw new IllegalArgumentException("El estado de cuenta es obligatorio");
        Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria");
        if ((resetTokenHash == null) != (resetTokenExpiry == null)) {
            throw new IllegalArgumentException("El hash y la expiración del token deben definirse juntos");
        }
        validarMaximo(resetTokenHash, MAX_RESET_TOKEN_HASH, "El hash del token de recuperación", true);

        this.id = id;
        this.nombreCompleto = nombreCompleto.trim();
        this.email = normalizedEmail;
        this.passwordHash = passwordHash;
        this.role = role;
        this.accountStatus = accountStatus;
        this.fechaCreacion = fechaCreacion;
        this.resetTokenHash = resetTokenHash;
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public static Usuario registrar(String nombreCompleto, String email, String passwordHash, Instant fechaCreacion) {
        return new Usuario(null, nombreCompleto, email, passwordHash, RolUsuario.USER,
                EstadoCuenta.ACTIVO, fechaCreacion, null, null);
    }

    public static Usuario registrarAdministradorInicial(String nombreCompleto, String email,
            String passwordHash, Instant fechaCreacion) {
        return new Usuario(null, nombreCompleto, email, passwordHash, RolUsuario.ADMIN,
                EstadoCuenta.ACTIVO, fechaCreacion, null, null);
    }

    public Usuario actualizarDatos(String nuevoNombreCompleto, String nuevoEmail) {
        return new Usuario(id, nuevoNombreCompleto, nuevoEmail, passwordHash, role,
                accountStatus, fechaCreacion, resetTokenHash, resetTokenExpiry);
    }

    public Usuario conTokenRecuperacion(String tokenHash, Instant expiry) {
        return new Usuario(id, nombreCompleto, email, passwordHash, role, accountStatus,
                fechaCreacion, tokenHash, expiry);
    }

    public Usuario limpiarTokenRecuperacion() {
        return new Usuario(id, nombreCompleto, email, passwordHash, role, accountStatus,
                fechaCreacion, null, null);
    }

    public Usuario actualizarAcceso(RolUsuario nuevoRole, EstadoCuenta nuevoEstado) {
        return new Usuario(id, nombreCompleto, email, passwordHash, nuevoRole, nuevoEstado,
                fechaCreacion, resetTokenHash, resetTokenExpiry);
    }

    public static String normalizarEmail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("El correo es obligatorio");
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_EMAIL || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("El correo no es válido");
        }
        int at = normalized.indexOf('@');
        if (at <= 0 || at != normalized.lastIndexOf('@') || at == normalized.length() - 1) {
            throw new IllegalArgumentException("El correo no es válido");
        }
        return normalized;
    }

    private static void validarMaximo(String value, int max, String field, boolean nullable) {
        if (value == null && nullable) return;
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " es obligatorio");
        if (value.length() > max) throw new IllegalArgumentException(field + " supera la longitud permitida");
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
