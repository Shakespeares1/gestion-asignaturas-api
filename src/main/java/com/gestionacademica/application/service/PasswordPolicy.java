package com.gestionacademica.application.service;

import java.nio.charset.StandardCharsets;

/** Shared password policy for account creation and password recovery. */
public final class PasswordPolicy {
    public static final int MIN_CODE_POINTS = 12;
    public static final int MAX_BCRYPT_BYTES = 72;

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        return password != null
                && !password.isBlank()
                && password.codePointCount(0, password.length()) >= MIN_CODE_POINTS
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_BYTES;
    }

    public static String validationMessage() {
        return "La contraseña debe tener al menos 12 caracteres y no superar 72 bytes UTF-8";
    }
}
