package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.validation.SignaturePng;
import com.kupanga.api.immobilier.validation.SignaturePngValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignatureDTO(

        @NotBlank(message = "La signature est obligatoire")
        @Size(min = 100, max = SignaturePngValidator.TAILLE_MAX_BASE64, message = "La signature semble invalide")
        @SignaturePng
        String signatureBase64
) {}
