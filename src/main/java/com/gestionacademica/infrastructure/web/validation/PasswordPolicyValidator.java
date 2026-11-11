package com.gestionacademica.infrastructure.web.validation;

import com.gestionacademica.application.service.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        return PasswordPolicy.isValid(value);
    }
}
