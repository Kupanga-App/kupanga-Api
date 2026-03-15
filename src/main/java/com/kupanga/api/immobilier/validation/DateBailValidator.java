package com.kupanga.api.immobilier.validation;

import com.kupanga.api.immobilier.dto.formDTO.ContratFormDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateBailValidator implements ConstraintValidator<DateBailValide, ContratFormDTO> {

    @Override
    public boolean isValid(ContratFormDTO dto, ConstraintValidatorContext ctx) {
        // Si dateFin est null → bail illimité → pas de validation croisée
        if (dto.getDateFin() == null) return true;

        // dateFin doit être strictement après dateDebut
        if (dto.getDateDebut() != null && dto.getDateFin().isAfter(dto.getDateDebut())) {
            return true;
        }

        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(
                "La date de fin doit être strictement après la date de début"
        ).addPropertyNode("dateFin").addConstraintViolation();

        return false;
    }
}