package com.kupanga.api.backoffice.dto;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.immobilier.entity.StatutQuittance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuittanceAdminDTO(
        Long            id,
        String          mois,
        Integer         annee,
        BigDecimal      montantTotal,
        Devise          devise,
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
                q.getDevise(),
                q.getStatut(),
                q.getLocataire() != null ? q.getLocataire().getMail() : "—",
                q.getCreatedAt()
        );
    }
}
