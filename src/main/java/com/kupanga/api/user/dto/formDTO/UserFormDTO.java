package com.kupanga.api.user.dto.formDTO;

import com.kupanga.api.user.entity.Role;
import lombok.Builder;

@Builder
public record UserFormDTO(

        String nom,
        String prenom,
        String email,
        String motDePasse,
        Role role
) {
}
