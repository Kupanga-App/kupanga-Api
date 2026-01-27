package com.kupanga.api.authentification.dto;

import lombok.Builder;

@Builder
public record LoginDTO(

        String email,
        String motDepasse
) {
}
