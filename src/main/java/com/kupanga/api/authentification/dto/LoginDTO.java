package com.kupanga.api.authentification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record LoginDTO(

        @NotBlank(message = "L'e-mail ne peut pas être vide")
        @Email(message = "L'e-mail doit être valide")
        String email,

        @NotBlank(message = "Le mot de passe ne peut pas être vide")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, " +
                        "une majuscule, une minuscule et un chiffre"
        )
        String password
) {
}
