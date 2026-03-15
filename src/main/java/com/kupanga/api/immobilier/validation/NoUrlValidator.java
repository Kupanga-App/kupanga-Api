package com.kupanga.api.immobilier.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NoUrlValidator implements ConstraintValidator<NoUrl, String> {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(https?|ftp)://\\S+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CRLF_PATTERN = Pattern.compile("[\\r\\n]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return true;

        if (URL_PATTERN.matcher(value).find()) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                    "Le champ ne doit pas contenir d'URL"
            ).addConstraintViolation();
            return false;
        }

        if (CRLF_PATTERN.matcher(value).find()) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                    "Le champ contient des caractères de contrôle interdits"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}