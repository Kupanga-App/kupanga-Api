package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.entity.TypeEtat;
import com.kupanga.api.immobilier.research.sort.EtatDesLieuxSortEnum;
import com.kupanga.api.pagination.Pagination;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Sort;

public record EtatDesLieuxSearchDTO(

        // ─── Filtres ──────────────────────────────────────────────────────────
        TypeEtat   type,
        StatutEdl  statut,
        Integer    annee,
        Integer    mois,
        Long       bienId,

        // ─── Pagination + tri ─────────────────────────────────────────────────
        @Min(value = 0, message = "La page doit être positive ou nulle")
        Integer        page,
        @Min(value = 1, message = "La taille de page doit être comprise entre 1 et 50")
        @Max(value = 50, message = "La taille de page doit être comprise entre 1 et 50")
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
