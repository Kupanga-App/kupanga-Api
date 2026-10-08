package com.kupanga.api.authentification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ResetPasswordDTO(

        @NotBlank(message = "Le token ne peut pas être vide")
        @Size(max = 64, message = "Token invalide")
        String token,

        @NotBlank(message = "Le mot de passe ne peut pas être vide")
        @Size(max = 72, message = "Le mot de passe ne doit pas dépasser 72 caractères")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, " +
                        "une majuscule, une minuscule et un chiffre"
        )
        String newPassword
) {
}
