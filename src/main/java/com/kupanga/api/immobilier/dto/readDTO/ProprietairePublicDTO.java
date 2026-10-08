package com.kupanga.api.immobilier.dto.readDTO;

import lombok.Builder;

/**
 * Propriétaire tel qu'il apparaît dans la vue publique d'un bien (P0-6) :
 * prénom, initiale du nom et photo uniquement. Jamais d'e-mail, d'id ni de rôle.
 */
@Builder
public record ProprietairePublicDTO(
        String firstName,
        String initialeNom,
        String urlProfile
) {
}
