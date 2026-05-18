package com.kupanga.api.backoffice.dto;

import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.entity.TypeEtat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EdlAdminDTO(
        Long          id,
        TypeEtat      type,
        LocalDate     dateRealisation,
        StatutEdl     statut,
        String        locataireMail,
        LocalDateTime createdAt
) {
    public static EdlAdminDTO from(EtatDesLieux e) {
        return new EdlAdminDTO(
                e.getId(),
                e.getType(),
                e.getDateRealisation(),
                e.getStatut(),
                e.getLocataire() != null ? e.getLocataire().getMail() : "—",
                e.getCreatedAt()
        );
    }
}
