package com.kupanga.api.utilisateur.dto.readDTO;

import com.kupanga.api.utilisateur.entity.Role;
import lombok.Builder;

@Builder
public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String mail ,
        String password ,
        Role role
) {
}
