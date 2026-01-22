package com.kupanga.api.utilisateur.dto.readDTO;

import lombok.Builder;

@Builder
public record UtilisateurDTO(
        String email ,
        String motDePasse ,
        String role
) {
}
