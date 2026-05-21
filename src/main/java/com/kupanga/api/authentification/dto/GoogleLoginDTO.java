package com.kupanga.api.authentification.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginDTO(
        @NotBlank(message = "Le token Google est obligatoire")
        String idToken
) {}
