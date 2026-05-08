package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public record ContratPageDTO(
        List<ContratDTO> contenu,
        int     pageActuelle,
        int     totalPages,
        long    totalElements,
        boolean dernierePage,
        boolean premierePage
) {
    public static ContratPageDTO from(Page<ContratDTO> page) {
        return new ContratPageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}
