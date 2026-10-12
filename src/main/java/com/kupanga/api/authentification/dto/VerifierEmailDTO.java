package com.kupanga.api.authentification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/** A14 : jeton reçu dans le lien de vérification de l'adresse e-mail. */
@Builder
public record VerifierEmailDTO(

        @NotBlank(message = "Le token ne peut pas être vide")
        @Size(max = 64, message = "Token invalide")
        String token
) {
}
