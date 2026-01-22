package com.kupanga.api.utilisateur.dto.formDTO;

import com.kupanga.api.utilisateur.entity.Role;

public record UtilisateurFormDTO(
        String email ,
        Role role
) {
}
