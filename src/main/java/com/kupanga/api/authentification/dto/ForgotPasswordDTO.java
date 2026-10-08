package com.kupanga.api.authentification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ForgotPasswordDTO(

        @NotBlank(message = "L'e-mail ne peut pas être vide")
        @Email(message = "L'e-mail doit être valide")
        String email
) {
}
