package com.kupanga.api.user.dto.readDTO;

import com.kupanga.api.user.entity.Role;
import lombok.Builder;

@Builder
public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String mail ,
        Role role,
        String urlProfile,
        Boolean hasCompleteProfil,
        /** C6 : visible seulement par l'utilisateur lui-même et par l'autre partie d'un bien (vue privée). */
        String telephone
) {
}
