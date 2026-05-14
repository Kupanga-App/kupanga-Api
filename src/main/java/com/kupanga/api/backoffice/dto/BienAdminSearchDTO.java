package com.kupanga.api.backoffice.dto;

import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.pagination.Pagination;
import org.springframework.data.domain.Sort;

public record BienAdminSearchDTO(
        String   titre,
        String   ville,
        TypeBien typeBien,
        int      page,
        int      size
) {
    public Pagination toPagination() {
        return Pagination.builder()
                .page(page)
                .size(size)
                .sortBy("createdAt")
                .direction(Sort.Direction.DESC)
                .build();
    }
}
