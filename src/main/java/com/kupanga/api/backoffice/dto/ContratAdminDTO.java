package com.kupanga.api.backoffice.dto;

import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContratAdminDTO(
        Long          id,
        LocalDate     dateDebut,
        LocalDate     dateFin,
        Double        loyerMensuel,
        StatutContrat statut,
        String        locataireMail,
        LocalDateTime createdAt
) {
    public static ContratAdminDTO from(Contrat c) {
        return new ContratAdminDTO(
                c.getId(),
                c.getDateDebut(),
                c.getDateFin(),
                c.getLoyerMensuel(),
                c.getStatut(),
                c.getLocataire() != null ? c.getLocataire().getMail() : "—",
                c.getCreatedAt()
        );
    }
}
