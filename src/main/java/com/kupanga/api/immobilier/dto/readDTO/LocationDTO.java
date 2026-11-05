package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.StatutLocation;
import com.kupanga.api.juridiction.Pays;

import java.time.LocalDateTime;

/**
 * Location (réservation ou location en cours) vue par le propriétaire ou par le candidat.
 * Ni adresse e-mail ni numéro de téléphone (C6) : le candidat n'est partie au bien qu'à la signature du bail.
 *
 * @param statut statut affiché ({@code EXPIREE} si l'échéance de la réservation est passée)
 * @param dateExpiration fin de la réservation si le bail n'est pas signé avant
 */
public record LocationDTO(
        Long id,
        Long bienId,
        String titreBien,
        String adresseBien,
        Pays pays,
        Long locataireId,
        String nomLocataire,
        String nomProprietaire,
        StatutLocation statut,
        LocalDateTime dateReservation,
        LocalDateTime dateExpiration,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        // Fin de bail (V55)
        java.time.LocalDate dateFinPrevue,
        com.kupanga.api.immobilier.entity.PartieLocation congeDonnePar,
        java.math.BigDecimal depotRestitue,
        String motifRetenue
) {
}
