package com.kupanga.api.authentification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/** A14 : demande d'un nouveau lien de vérification de l'adresse e-mail. */
@Builder
public record RenvoyerVerificationDTO(

        @NotBlank(message = "L'e-mail ne peut pas être vide")
        @Email(message = "L'e-mail doit être valide")
        @Size(max = 255)
        String email
) {
}
