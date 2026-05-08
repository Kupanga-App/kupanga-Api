package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public record QuittancePageDTO(
        List<QuittanceDTO> contenu,
        int     pageActuelle,
        int     totalPages,
        long    totalElements,
        boolean dernierePage,
        boolean premierePage
) {
    public static QuittancePageDTO from(Page<QuittanceDTO> page) {
        return new QuittancePageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}
