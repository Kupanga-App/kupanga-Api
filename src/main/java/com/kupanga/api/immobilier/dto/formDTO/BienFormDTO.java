package com.kupanga.api.immobilier.dto.formDTO;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record BienFormDTO(

        @NotBlank(message = "Le titre ne peut pas être vide")
        @Size(min = 5, max = 100, message = "Le titre doit contenir entre 5 et 100 caractères")
        String titre,

        @NotBlank(message = "L'adresse ne peut pas être vide")
        String adresse,

        @NotBlank(message = "La ville ne peut pas être vide")
        String ville,

        @NotBlank(message = "Le code postal ne peut pas être vide")
        @Pattern(regexp = "^\\d{5}$", message = "Le code postal doit contenir 5 chiffres")
        String codePostal,

        @DecimalMin(value = "-90.0", message = "La latitude doit être supérieure ou égale à -90")
        @DecimalMax(value = "90.0", message = "La latitude doit être inférieure ou égale à 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "La longitude doit être supérieure ou égale à -180")
        @DecimalMax(value = "180.0", message = "La longitude doit être inférieure ou égale à 180")
        Double longitude,

        @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
        String description,

        @NotNull(message = "L'ID du propriétaire est obligatoire")
        @Positive(message = "L'ID du propriétaire doit être un nombre positif")
        Long proprietaireId

) {}
