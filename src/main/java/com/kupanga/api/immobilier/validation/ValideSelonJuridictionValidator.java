package com.kupanga.api.immobilier.validation;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.ViolationChamp;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * J4 : applique {@link JuridictionRegistry#controlerBien} à la création d'un bien et rattache chaque refus à son
 * champ (400 avec le détail par champ, comme les autres contraintes).
 * <p>
 * Hors contexte Spring (validateur par défaut des tests) ou sans registre, rien n'est contrôlé ici :
 * {@code BienServiceImpl} refait le même contrôle avant tout enregistrement.
 */
public class ValideSelonJuridictionValidator implements ConstraintValidator<ValideSelonJuridiction, BienFormDTO> {

    private final JuridictionRegistry registre;

    public ValideSelonJuridictionValidator() {
        this.registre = null;
    }

    @Autowired
    public ValideSelonJuridictionValidator(ObjectProvider<JuridictionRegistry> registre) {
        this.registre = registre.getIfAvailable();
    }

    /** Tests. */
    public ValideSelonJuridictionValidator(JuridictionRegistry registre) {
        this.registre = registre;
    }

    @Override
    public boolean isValid(BienFormDTO dto, ConstraintValidatorContext ctx) {
        if (dto == null || registre == null) {
            return true;
        }
        List<ViolationChamp> violations = registre.controlerBien(dto.getPays(), dto, true);
        if (violations.isEmpty()) {
            return true;
        }
        ctx.disableDefaultConstraintViolation();
        // Messages construits par le registre, sans valeur saisie : pas d'interpolation de données utilisateur
        violations.forEach(violation -> ctx
                .buildConstraintViolationWithTemplate(echapper(violation.message()))
                .addPropertyNode(violation.champ())
                .addConstraintViolation());
        return false;
    }

    /** Neutralise les caractères d'interpolation des messages Bean Validation (accolades, dollar, antislash). */
    private static String echapper(String message) {
        return message.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}").replace("$", "\\$");
    }
}
