package com.kupanga.api.immobilier.research.dto;

import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.research.sort.BienSortEnum;
import com.kupanga.api.pagination.Pagination;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

public record BienSearchDTO(

        // ─── Localisation ─────────────────────────────────────────────────────
        List<String>        villes,
        List<String>        pays,
        List<String>        codesPostaux,

        // ─── Type de bien ─────────────────────────────────────────────────────
        List<TypeBien>      typesBien,
        String              titre,

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
        List<ClasseEnergie> classesEnergie,
        List<ClasseGes>     classesGes,

        // ─── Chauffage ────────────────────────────────────────────────────────
        List<ModeChauffage> modesChauffage,

        // ─── POI ──────────────────────────────────────────────────────────────
        List<PoiType>       poisRequis,

        // ─── Pagination + tri ─────────────────────────────────────────────────
        Integer             page,
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