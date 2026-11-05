package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.user.dto.readDTO.UserDTO;

import java.time.LocalDateTime;

/**
 * Réservation en cours d'un bien, dans la vue privée du propriétaire ({@code BienDTO.reservation}) : sert à
 * préparer le bail du candidat. Le candidat n'a ni téléphone (C6) ni accès à cette vue.
 *
 * @param locataire candidat qui a réservé (id, nom, prénom, e-mail, photo ; l'e-mail est déjà connu du
 *                  propriétaire par la messagerie)
 */
public record ReservationBienDTO(
        Long id,
        UserDTO locataire,
        LocalDateTime dateReservation,
        LocalDateTime dateExpiration
) {
}
