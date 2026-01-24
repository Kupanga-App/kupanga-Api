package com.kupanga.api.login.dto;

import lombok.Builder;

@Builder
public record AuthResponseDTO(

        String accessToken
) {}

