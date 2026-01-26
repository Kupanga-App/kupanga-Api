package com.kupanga.api.pagination.model;

import lombok.*;
import org.springframework.data.domain.Sort;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Pagination {

    private int page = 0;       // page par défaut 1
    private int size = 10;      // taille par défaut
    private String sortBy = "id"; // tri par défaut
    private Sort.Direction direction = Sort.Direction.ASC; // ordre par défaut
}
