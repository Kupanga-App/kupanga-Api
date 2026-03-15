package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public record BienPageDTO(
        List<BienDTO> contenu,
        int pageActuelle,
        int totalPages,
        long totalElements,
        boolean dernierePage,
        boolean premierePage
) {
    public static BienPageDTO from(Page<BienDTO> page) {
        return new BienPageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}
