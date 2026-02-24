package com.kupanga.api.immobilier.dto.readDTO;

import lombok.Builder;

@Builder
public record LocataireSimpleDTO(
        Long id,
        String nom,
        String urlProfile
) {}
