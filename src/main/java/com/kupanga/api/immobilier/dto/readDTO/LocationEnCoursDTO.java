package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.PartieLocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Location en cours d'un bien, dans sa vue privée (propriétaire ou locataire actuel) : fin de bail (2026-11-05).
 *
 * @param dateFinPrevue  date annoncée par le congé (null : pas de congé)
 * @param congeDonnePar  qui a donné congé (null : pas de congé)
 * @param depotGarantie  dépôt du bail signé, plafond du montant restitué (null si aucun bail signé n'est trouvé)
 */
public record LocationEnCoursDTO(
        Long id,
        LocalDateTime dateDebut,
        LocalDate dateFinPrevue,
        PartieLocation congeDonnePar,
        BigDecimal depotGarantie
) {
}
