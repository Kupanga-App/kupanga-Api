package com.kupanga.api.user.dto.readDTO;

import lombok.Builder;

@Builder
public record AvatarProfilDTO(
        Long id,
        String url
) {
}
