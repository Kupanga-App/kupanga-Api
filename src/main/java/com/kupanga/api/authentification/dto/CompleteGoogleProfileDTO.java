package com.kupanga.api.authentification.dto;

import com.kupanga.api.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record CompleteGoogleProfileDTO(
        @NotNull(message = "Le rôle est obligatoire")
        Role role
) {}
