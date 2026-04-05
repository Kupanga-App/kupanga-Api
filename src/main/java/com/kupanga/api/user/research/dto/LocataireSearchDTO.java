package com.kupanga.api.user.research.dto;

import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.user.research.sort.LocataireSortEnum;
import org.springframework.data.domain.Sort;

public record LocataireSearchDTO(

        // ─── Filtres dynamiques ───────────────────────────────────────────────
        String firstName,
        String lastName,
        String mail,

        // ─── Pagination + tri ─────────────────────────────────────────────────
        Integer        page,
        Integer        size,
        String         sortBy,
        Sort.Direction sortDirection

) {
    public LocataireSearchDTO {
        page          = page          != null ? page          : 0;
        size          = size          != null ? size          : 10;
        sortBy        = LocataireSortEnum.resolveField(sortBy);
        sortDirection = sortDirection != null ? sortDirection : Sort.Direction.ASC;
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