package com.kupanga.api.utilisateur.research.researchDTO;

import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
public class AvatarProfileResearchDTO {

    private Optional<Integer> pageNumber;// pour filtrer par numéro de la page
    private Optional<Integer> pageSize ;
    private Optional<String> sort;
}
