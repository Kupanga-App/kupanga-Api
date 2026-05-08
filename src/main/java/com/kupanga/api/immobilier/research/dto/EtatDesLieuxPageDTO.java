package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public record EtatDesLieuxPageDTO(
        List<EtatDesLieuxDTO> contenu,
        int     pageActuelle,
        int     totalPages,
        long    totalElements,
        boolean dernierePage,
        boolean premierePage
) {
    public static EtatDesLieuxPageDTO from(Page<EtatDesLieuxDTO> page) {
        return new EtatDesLieuxPageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}
