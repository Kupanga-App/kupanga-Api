package com.kupanga.api.immobilier.research.dto;


import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.research.sort.BienSortEnum;
import org.springframework.data.domain.Sort;

import java.util.List;

public record BienSearchDTO(

        // Critères de recherche

        List<String>   villes,
        List<String>   pays,
        List<String>   codesPostaux,
        List<TypeBien> typesBien,
        String         titre,

        // Pagination + tri

        Integer        page,
        Integer        size,
        String         sortBy,
        Sort.Direction sortDirection

) {
    // Valeurs par défaut via compact constructor
    public BienSearchDTO {
        page          = page          != null ? page          : 0;
        size          = size          != null ? size          : 10;
        sortBy        = BienSortEnum.resolveField(sortBy);
        sortDirection = sortDirection != null ? sortDirection : Sort.Direction.ASC;
    }

    // Convertit en objet Pagination
    public Pagination toPagination() {
        return Pagination.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(sortDirection)
                .build();
    }
}
