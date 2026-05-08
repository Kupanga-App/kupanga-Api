package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.research.sort.ContratSortEnum;
import com.kupanga.api.pagination.Pagination;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

public record ContratSearchDTO(

        // ─── Filtres ──────────────────────────────────────────────────────────
        Long          bienId,
        Integer       dureeBailMoisMin,
        Integer       dureeBailMoisMax,
        Double        loyerMin,
        Double        loyerMax,
        LocalDate     dateDebutApres,
        LocalDate     dateDebutAvant,
        StatutContrat statut,

        // ─── Pagination + tri ─────────────────────────────────────────────────
        Integer        page,
        Integer        size,
        String         sortBy,
        Sort.Direction sortDirection

) {
    public ContratSearchDTO {
        page          = page          != null ? page          : 0;
        size          = size          != null ? size          : 10;
        sortBy        = ContratSortEnum.resolveField(sortBy);
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
