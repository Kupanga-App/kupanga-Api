package com.kupanga.api.immobilier.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * B6 : signature manuscrite = image PNG en base64 (sans préfixe {@code data:}), de taille et de dimensions bornées.
 * Elle est insérée telle quelle dans les PDF (contrat, EDL, quittance) : une image aux dimensions énormes
 * ferait exploser la mémoire au rendu.
 */
@Documented
@Constraint(validatedBy = SignaturePngValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SignaturePng {

    String message() default "La signature doit être une image PNG valide";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
