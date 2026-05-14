package com.kupanga.api.backoffice.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record BienAdminPageDTO(
        List<BienAdminDTO> contenu,
        int                pageActuelle,
        int                totalPages,
        long               totalElements,
        boolean            dernierePage,
        boolean            premierePage
) {
    public static BienAdminPageDTO from(Page<BienAdminDTO> page) {
        return new BienAdminPageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}
