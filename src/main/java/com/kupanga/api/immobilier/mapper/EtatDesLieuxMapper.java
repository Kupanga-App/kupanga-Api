package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EtatDesLieuxMapper {

    // ─────────────────────────────────────────────────────────────
    // EtatDesLieux → DTO
    // ─────────────────────────────────────────────────────────────

    @Mapping(target = "nomProprietaire", source = "proprietaire")
    @Mapping(target = "emailProprietaire", source = "proprietaire.mail")

    @Mapping(target = "nomLocataire", source = "locataire")
    @Mapping(target = "emailLocataire", source = "locataire.mail")

    @Mapping(target = "adresseBien", source = "bien")
    @Mapping(target = "typeBien", expression =
            "java(edl.getBien() != null && edl.getBien().getTypeBien() != null ? edl.getBien().getTypeBien().name() : null)"
    )

    EtatDesLieuxDTO toDTO(EtatDesLieux edl);

    // ─────────────────────────────────────────────────────────────
    // PieceEdl → PieceEdlDTO
    // ─────────────────────────────────────────────────────────────

    EtatDesLieuxDTO.PieceEdlDTO toDTO(PieceEdl piece);

    // ─────────────────────────────────────────────────────────────
    // ElementEdl → ElementEdlDTO
    // ─────────────────────────────────────────────────────────────

    @Mapping(target = "typeElement", expression =
            "java(element.getTypeElement() != null ? element.getTypeElement().name() : null)"
    )
    @Mapping(target = "etatElement", expression =
            "java(element.getEtatElement() != null ? element.getEtatElement().name() : null)"
    )
    EtatDesLieuxDTO.ElementEdlDTO toDTO(ElementEdl element);

    // ─────────────────────────────────────────────────────────────
    // CompteurReleve → CompteurReleveDTO
    // ─────────────────────────────────────────────────────────────

    @Mapping(target = "typeCompteur", expression =
            "java(compteur.getTypeCompteur() != null ? compteur.getTypeCompteur().name() : null)"
    )
    EtatDesLieuxDTO.CompteurReleveDTO toDTO(CompteurReleve compteur);

    // ─────────────────────────────────────────────────────────────
    // CleRemise → CleRemiseDTO
    // ─────────────────────────────────────────────────────────────

    EtatDesLieuxDTO.CleRemiseDTO toDTO(CleRemise cle);

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    default String map(User user) {
        if (user == null) {
            return null;
        }

        return user.getFirstName() + " " + user.getLastName();
    }

    default String map(Bien bien) {
        if (bien == null) {
            return null;
        }

        return bien.getAdresse()
                + ", "
                + bien.getCodePostal()
                + " "
                + bien.getVille();
    }
}