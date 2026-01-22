package com.kupanga.api.login.dto;

import com.kupanga.api.utilisateur.entity.Role;
import lombok.Builder;

@Builder
public record AuthResponseDTO(

        String jwtToken,
        String refreshToken,
        Role role,
        String email
) {}

