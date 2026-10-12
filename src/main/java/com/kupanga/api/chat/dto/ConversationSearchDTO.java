package com.kupanga.api.chat.dto;

import com.kupanga.api.chat.sort.ConversationSortEnum;
import com.kupanga.api.pagination.Pagination;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;

public record ConversationSearchDTO(

        @Size(max = 100) String nomDuBien,
        Boolean lu,
        // ─── Pagination + tri ─────────────────────────────────────────────────
        @Min(value = 0, message = "La page doit être positive ou nulle")
        Integer        page,
        @Min(value = 1, message = "La taille de page doit être comprise entre 1 et 50")
        @Max(value = 50, message = "La taille de page doit être comprise entre 1 et 50")
        Integer        size,
        String         sortBy,
        Sort.Direction sortDirection
) {

    public ConversationSearchDTO {
        page          = page          != null ? page          : 0;
        size          = size          != null ? size          : 10;
        sortBy        = ConversationSortEnum.resolveField(sortBy);
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
