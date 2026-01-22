package com.kupanga.api.login.dto;

import lombok.Builder;

@Builder
public record LoginDTO(

        String email,
        String motDepasse
) {
}
