package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.StatutDemandeLocation;
import com.kupanga.api.juridiction.Pays;

import java.time.LocalDateTime;

/**
 * Demande de location vue par le propriétaire ou par le locataire. Aucune adresse e-mail ni numéro de téléphone :
 * le candidat n'est pas encore partie au bien (C6).
 *
 * @param statut statut affiché ({@code EXPIREE} si le délai est dépassé, même si la base dit encore {@code EN_ATTENTE})
 */
public record DemandeLocationDTO(
        Long id,
        Long bienId,
        String titreBien,
        String adresseBien,
        Pays pays,
        Long locataireId,
        String nomLocataire,
        String nomProprietaire,
        StatutDemandeLocation statut,
        LocalDateTime dateCreation,
        LocalDateTime dateExpiration,
        LocalDateTime dateReponse
) {
}
