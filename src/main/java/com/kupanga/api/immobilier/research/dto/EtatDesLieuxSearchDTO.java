package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.entity.TypeEtat;
import com.kupanga.api.immobilier.research.sort.EtatDesLieuxSortEnum;
import com.kupanga.api.pagination.Pagination;
import org.springframework.data.domain.Sort;

public record EtatDesLieuxSearchDTO(

        // ─── Filtres ──────────────────────────────────────────────────────────
        TypeEtat   type,
        StatutEdl  statut,
        Integer    annee,
        Integer    mois,
        Long       bienId,

        // ─── Pagination + tri ─────────────────────────────────────────────────
        Integer        page,
        Integer        size,
        String         sortBy,
        Sort.Direction sortDirection

) {
    public EtatDesLieuxSearchDTO {
        page          = page          != null ? page          : 0;
        size          = size          != null ? size          : 10;
        sortBy        = EtatDesLieuxSortEnum.resolveField(sortBy);
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
