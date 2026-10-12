package com.kupanga.api.user.validation;

import com.kupanga.api.user.utils.TelephoneUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelephoneValidator implements ConstraintValidator<Telephone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        return TelephoneUtils.estValide(value);
    }
}
