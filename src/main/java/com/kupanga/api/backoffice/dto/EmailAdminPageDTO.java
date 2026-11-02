package com.kupanga.api.backoffice.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record EmailAdminPageDTO(
        List<EmailAdminDTO> contenu,
        int                 pageActuelle,
        int                 totalPages,
        long                totalElements,
        boolean             dernierePage,
        boolean             premierePage
) {
    public static EmailAdminPageDTO from(Page<EmailAdminDTO> page) {
        return new EmailAdminPageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}
