package com.kupanga.api.user.dto.formDTO;

import com.kupanga.api.user.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record UserFormDTO(

        @NotBlank(message = "Le prénom ne peut pas être vide")
        String firstName,

        @NotBlank(message = "Le nom ne peut pas être vide")
        String lastName,

        @NotBlank(message = "L'e-mail ne peut pas être vide")
        @Email(message = "L'e-mail doit être valide")
        String mail,

        @NotBlank(message = "Le mot de passe ne peut pas être vide")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, " +
                        "une majuscule, une minuscule et un chiffre"
        )
        String password ,

        @NotNull(message = "Un rôle valide est nécessaire")
        Role role,

        String urlProfile
) {}

