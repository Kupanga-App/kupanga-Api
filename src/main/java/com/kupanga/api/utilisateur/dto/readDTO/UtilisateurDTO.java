package com.kupanga.api.utilisateur.dto.readDTO;

import com.kupanga.api.utilisateur.entity.Role;
import lombok.Builder;

@Builder
public record UtilisateurDTO(
        String email ,
        String motDePasse ,
        Role role
) {
}
