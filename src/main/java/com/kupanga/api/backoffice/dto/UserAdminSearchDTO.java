package com.kupanga.api.backoffice.dto;

import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.user.entity.Role;
import org.springframework.data.domain.Sort;

public record UserAdminSearchDTO(
        String firstName,
        String lastName,
        String mail,
        Role   role,
        int    page,
        int    size
) {
    public Pagination toPagination() {
        return Pagination.builder()
                .page(page)
                .size(size)
                .sortBy("id")
                .direction(Sort.Direction.DESC)
                .build();
    }
}
