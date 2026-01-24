package com.kupanga.api.utilisateur.dto.formDTO;

import com.kupanga.api.utilisateur.entity.Role;

public record UserFormDTO(

        String nom,
        String prenom,
        String email,
        String motDePasse,
        Role role
) {
}
