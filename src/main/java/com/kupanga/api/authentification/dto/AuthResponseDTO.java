package com.kupanga.api.authentification.dto;

import lombok.Builder;

@Builder
public record AuthResponseDTO(

        String accessToken
) {}

