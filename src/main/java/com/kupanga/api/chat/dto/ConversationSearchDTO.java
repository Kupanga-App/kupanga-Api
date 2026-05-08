package com.kupanga.api.chat.dto;

import com.kupanga.api.chat.sort.ConversationSortEnum;
import com.kupanga.api.pagination.Pagination;
import org.springframework.data.domain.Sort;

public record ConversationSearchDTO(

        String nomDuBien,
        Boolean lu,
        // ─── Pagination + tri ─────────────────────────────────────────────────
        Integer        page,
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
