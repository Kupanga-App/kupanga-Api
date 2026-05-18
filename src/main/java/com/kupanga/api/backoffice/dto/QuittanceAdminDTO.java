package com.kupanga.api.backoffice.dto;

import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.immobilier.entity.StatutQuittance;

import java.time.LocalDateTime;

public record QuittanceAdminDTO(
        Long            id,
        String          mois,
        Integer         annee,
        Double          montantTotal,
        StatutQuittance statut,
        String          locataireMail,
        LocalDateTime   createdAt
) {
    public static QuittanceAdminDTO from(Quittance q) {
        return new QuittanceAdminDTO(
                q.getId(),
                q.getMois(),
                q.getAnnee(),
                q.getMontantTotal(),
                q.getStatut(),
                q.getLocataire() != null ? q.getLocataire().getMail() : "—",
                q.getCreatedAt()
        );
    }
}
