package com.kupanga.api.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** C6 : numéro international facultatif (vide accepté), normalisé ensuite par {@code TelephoneUtils}. */
@Documented
@Constraint(validatedBy = TelephoneValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Telephone {

    String message() default "Le numéro doit être au format international, par exemple +243 812 345 678";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
