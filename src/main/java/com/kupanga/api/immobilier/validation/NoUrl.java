package com.kupanga.api.immobilier.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoUrlValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoUrl {

    String message() default "Le champ ne doit pas contenir d'URL";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}