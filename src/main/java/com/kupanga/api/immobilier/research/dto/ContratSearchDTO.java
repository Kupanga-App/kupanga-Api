package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.research.sort.ContratSortEnum;
import com.kupanga.api.pagination.Pagination;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratSearchDTO(

        // ─── Filtres ──────────────────────────────────────────────────────────
        Long          bienId,
        Integer       dureeBailMoisMin,
        Integer       dureeBailMoisMax,
        @DecimalMin("0") @DecimalMax("9999999999.99") BigDecimal    loyerMin,
        @DecimalMin("0") @DecimalMax("9999999999.99") BigDecimal    loyerMax,
        LocalDate     dateDebutApres,
        LocalDate     dateDebutAvant,
        StatutContrat statut,

        // ─── Pagination + tri ─────────────────────────────────────────────────
        @Min(value = 0, message = "La page doit être positive ou nulle")
        Integer        page,
        @Min(value = 1, message = "La taille de page doit être comprise entre 1 et 50")
        @Max(value = 50, message = "La taille de page doit être comprise entre 1 et 50")
        Integer        size,
        String         sortBy,
        Sort.Direction sortDirection

) {
    public ContratSearchDTO {
        page          = page          != null ? page          : 0;
        size          = size          != null ? size          : 10;
        sortBy        = ContratSortEnum.resolveField(sortBy);
        sortDirection = sortDirection != null ? sortDirection : Sort.Direction.DESC;
    }

    public Pagination toPagination() {
        return Pagination.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(sortDirection)
                .build();
    }
}
