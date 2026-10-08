package com.kupanga.api.user.dto.formDTO;

import com.kupanga.api.user.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record UserFormDTO(

        @NotBlank(message = "Le prénom ne peut pas être vide")
        @Size(max = 50, message = "Le prénom ne doit pas dépasser 50 caractères")
        @Pattern(regexp = "^[\\p{L} '\\-]+$", message = "Le prénom ne doit contenir que des lettres, espaces, apostrophes ou tirets")
        String firstName,

        @NotBlank(message = "Le nom ne peut pas être vide")
        @Size(max = 50, message = "Le nom ne doit pas dépasser 50 caractères")
        @Pattern(regexp = "^[\\p{L} '\\-]+$", message = "Le nom ne doit contenir que des lettres, espaces, apostrophes ou tirets")
        String lastName,

        @NotBlank(message = "L'e-mail ne peut pas être vide")
        @Email(message = "L'e-mail doit être valide")
        @Size(max = 255, message = "L'e-mail ne doit pas dépasser 255 caractères")
        String mail,

        @NotBlank(message = "Le mot de passe ne peut pas être vide")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, " +
                        "une majuscule, une minuscule et un chiffre"
        )
        @Size(max = 72, message = "Le mot de passe ne doit pas dépasser 72 caractères")
        String password ,

        @NotNull(message = "Un rôle valide est nécessaire")
        Role role,

        String urlAvatar
) {}

