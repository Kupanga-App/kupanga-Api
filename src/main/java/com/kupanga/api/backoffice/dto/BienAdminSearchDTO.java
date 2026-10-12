package com.kupanga.api.backoffice.dto;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.pagination.Pagination;
import org.springframework.data.domain.Sort;

public record BienAdminSearchDTO(
        String   titre,
        String   ville,
        Pays     pays,
        TypeBien typeBien,
        int      page,
        int      size
) {
    /** Taille de page maximale du back-office. */
    public static final int SIZE_MAX = 100;

    /** Paramètres hors bornes ramenés dans les limites (pas de 500 sur page=-1 / size=0). */
    public BienAdminSearchDTO {
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), SIZE_MAX);
    }

    public Pagination toPagination() {
        return Pagination.builder()
                .page(page)
                .size(size)
                .sortBy("createdAt")
                .direction(Sort.Direction.DESC)
                .build();
    }
}
