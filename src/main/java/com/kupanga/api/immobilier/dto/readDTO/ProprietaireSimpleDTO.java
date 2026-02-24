package com.kupanga.api.immobilier.dto.readDTO;

import lombok.Builder;

@Builder
public record ProprietaireSimpleDTO(
        Long id,
        String nom,
        String urlProfile
) {}
