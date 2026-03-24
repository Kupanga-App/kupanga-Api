package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.entity.Quittance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface QuittanceMapper {

    @Mapping(target = "moisLabel", source = ".", qualifiedByName = "buildMoisLabel")
    @Mapping(target = "nomProprietaire", source = ".", qualifiedByName = "buildNomProprietaire")
    @Mapping(target = "emailProprietaire", source = "proprietaire.mail")
    @Mapping(target = "nomLocataire", source = ".", qualifiedByName = "buildNomLocataire")
    @Mapping(target = "emailLocataire", source = "locataire.mail")
    @Mapping(target = "adresseBien", source = "bien", qualifiedByName = "buildAdresseBien")
    @Mapping(target = "typeBien", source = "bien", qualifiedByName = "buildTypeBien")
    QuittanceDTO toDTO(Quittance q);

    // -------------------------
    // Méthodes custom propres
    // -------------------------

    @Named("buildMoisLabel")
    default String buildMoisLabel(Quittance q) {
        if (q.getMois() == null || q.getAnnee() == null) return "—";
        return q.getMois() + " " + q.getAnnee();
    }

    @Named("buildNomProprietaire")
    default String buildNomProprietaire(Quittance q) {
        return q.getProprietaire().getFirstName() + " " + q.getProprietaire().getLastName();
    }

    @Named("buildNomLocataire")
    default String buildNomLocataire(Quittance q) {
        return q.getLocataire().getFirstName() + " " + q.getLocataire().getLastName();
    }

    @Named("buildAdresseBien")
    default String buildAdresseBien(com.kupanga.api.immobilier.entity.Bien bien) {
        return bien.getAdresse() + ", " + bien.getCodePostal() + " " + bien.getVille();
    }

    @Named("buildTypeBien")
    default String buildTypeBien(com.kupanga.api.immobilier.entity.Bien bien) {
        return bien.getTypeBien() != null ? bien.getTypeBien().name() : null;
    }
}