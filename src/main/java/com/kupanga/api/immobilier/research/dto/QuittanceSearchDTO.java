package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.entity.StatutQuittance;
import com.kupanga.api.immobilier.research.sort.QuittanceSortEnum;
import com.kupanga.api.pagination.Pagination;
import org.springframework.data.domain.Sort;

public record QuittanceSearchDTO(

        // ─── Filtres ──────────────────────────────────────────────────────────
        Integer         annee,
        String         mois,
        StatutQuittance statut,
        Long            bienId,

        // ─── Pagination + tri ─────────────────────────────────────────────────
        Integer        page,
        Integer        size,
        String         sortBy,
        Sort.Direction sortDirection

) {
    public QuittanceSearchDTO {
        page          = page          != null ? page          : 0;
        size          = size          != null ? size          : 10;
        sortBy        = QuittanceSortEnum.resolveField(sortBy);
        sortDirection = sortDirection != null ? sortDirection : Sort.Direction.DESC;
    }

    public Pagination toPagination() {
        return Pagination.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(sortDirection)
                .build();
    }
}
