package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.research.sort.BienSortEnum;
import com.kupanga.api.pagination.Pagination;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

public record BienSearchDTO(

        // ─── Localisation ─────────────────────────────────────────────────────
        @Size(max = 20) List<@Size(max = 100) String> villes,
        @Size(max = 20) List<@Size(max = 100) String> pays,
        @Size(max = 20) List<@Size(max = 100) String> codesPostaux,

        // ─── Type de bien ─────────────────────────────────────────────────────
        @Size(max = 20) List<TypeBien> typesBien,
        @Size(max = 100) String titre,

        // ─── Conditions de location ───────────────────────────────────────────
        Double              loyerMin,
        Double              loyerMax,
        Boolean             meuble,
        Boolean             colocation,
        LocalDate           disponibleAvant,

        // ─── Caractéristiques physiques ───────────────────────────────────────
        Double              surfaceMin,
        Double              surfaceMax,
        Integer             piecesMin,
        Boolean             ascenseur,
        Integer             etageMin,
        Integer             etageMax,

        // ─── Diagnostic énergétique ───────────────────────────────────────────
        @Size(max = 20) List<ClasseEnergie> classesEnergie,
        @Size(max = 20) List<ClasseGes> classesGes,

        // ─── Chauffage ────────────────────────────────────────────────────────
        @Size(max = 20) List<ModeChauffage> modesChauffage,

        // ─── POI ──────────────────────────────────────────────────────────────
        @Size(max = 20) List<PoiType> poisRequis,

        // ─── Pagination + tri ─────────────────────────────────────────────────
        @Min(value = 0, message = "La page doit être positive ou nulle")
        Integer             page,
        @Min(value = 1, message = "La taille de page doit être comprise entre 1 et 50")
        @Max(value = 50, message = "La taille de page doit être comprise entre 1 et 50")
        Integer             size,
        String              sortBy,
        Sort.Direction      sortDirection

) {
    public BienSearchDTO {
        page          = page          != null ? page          : 0;
        size          = size          != null ? size          : 10;
        sortBy        = BienSortEnum.resolveField(sortBy);
        sortDirection = sortDirection != null ? sortDirection : Sort.Direction.ASC;
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