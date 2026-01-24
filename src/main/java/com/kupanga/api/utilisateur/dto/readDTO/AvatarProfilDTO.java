package com.kupanga.api.utilisateur.dto.readDTO;

import lombok.Builder;

@Builder
public record AvatarProfilDTO(
        Long id,
        String url
) {
}
