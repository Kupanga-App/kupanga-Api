package com.kupanga.api.immobilier.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * J4 (C1) : le formulaire de bien respecte le profil de juridiction de son pays (champs obligatoires et masqués,
 * types de bien, devise, plafonds), cf. {@code JuridictionRegistry#controlerBien}. Une erreur par champ fautif.
 */
@Documented
@Constraint(validatedBy = ValideSelonJuridictionValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValideSelonJuridiction {
    String message() default "Formulaire invalide pour le pays du bien";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
