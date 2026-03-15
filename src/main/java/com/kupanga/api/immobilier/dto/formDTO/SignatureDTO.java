package com.kupanga.api.immobilier.dto.formDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignatureDTO(

        @NotBlank(message = "La signature est obligatoire")
        @Size(min = 100, message = "La signature semble invalide")
        String signatureBase64
) {}
