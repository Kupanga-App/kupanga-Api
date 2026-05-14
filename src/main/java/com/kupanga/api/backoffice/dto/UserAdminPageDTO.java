package com.kupanga.api.backoffice.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record UserAdminPageDTO(
        List<UserAdminDTO> contenu,
        int                pageActuelle,
        int                totalPages,
        long               totalElements,
        boolean            dernierePage,
        boolean            premierePage
) {
    public static UserAdminPageDTO from(Page<UserAdminDTO> page) {
        return new UserAdminPageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}
