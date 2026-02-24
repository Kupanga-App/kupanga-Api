package com.kupanga.api.immobilier.dto.readDTO;

import lombok.Builder;

@Builder
public record BienResponseDTO(

        Long id,
        String titre,
        String adresse,
        String ville,
        String codePostal,
        Double latitude,
        Double longitude,
        String description,
        Boolean disponible,
        ProprietaireSimpleDTO proprietaire,
        LocataireSimpleDTO locataire

) {}
