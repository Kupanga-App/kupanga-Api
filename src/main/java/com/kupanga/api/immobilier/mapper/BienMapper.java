package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface BienMapper {

    @Mapping(target = "latitude",         expression = "java(bien.getLocalisation() != null ? bien.getLocalisation().getY() : null)")
    @Mapping(target = "longitude",        expression = "java(bien.getLocalisation() != null ? bien.getLocalisation().getX() : null)")
    @Mapping(target = "proprietaire",     source = "proprietaire", qualifiedByName = "mapProprietairePublic")
    @Mapping(target = "images",           source = "images",       qualifiedByName = "imageUrls")
    @Mapping(target = "pois",         source = "pois",         qualifiedByName = "mapPoisFr")
    // ─── Champs privés — jamais exposés publiquement ──────────────────────────
    @Mapping(target = "locataire",        ignore = true)
    @Mapping(target = "contrats",         ignore = true)
    @Mapping(target = "quittances",       ignore = true)
    @Mapping(target = "documents",        ignore = true)
    BienDTO toPublicDTO(Bien bien);

    // ─── Propriétaire public : prénom + nom uniquement ────────────────────────
    @Named("mapProprietairePublic")
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "password",     ignore = true)
    @Mapping(target = "mail",         ignore = true)
    @Mapping(target = "role",         ignore = true)
    @Mapping(target = "hasCompleteProfil", ignore = true)
    UserDTO mapProprietairePublic(User user);

    // ─── Images ───────────────────────────────────────────────────────────────
    @Named("imageUrls")
    default List<String> mapImages(Set<BienImage> images) {
        if (images == null) return List.of();
        return images.stream().map(BienImage::getUrl).toList();
    }

    // ─── Méthodes conservées pour le mapper privé (BienPriveMapper) ──────────
    @Named("contratUrls")
    default List<String> mapContrats(Set<Contrat> contrats) {
        if (contrats == null) return List.of();
        return contrats.stream().map(Contrat::getUrlPdf).toList();
    }

    @Named("quittanceUrls")
    default List<String> mapQuittances(Set<Quittance> quittances) {
        if (quittances == null) return List.of();
        return quittances.stream().map(Quittance::getUrlPdf).toList();
    }

    @Named("documentUrls")
    default List<String> mapDocuments(Set<Document> documents) {
        if (documents == null) return List.of();
        return documents.stream().map(Document::getUrl).toList();
    }

    // ─── POI → labels français ────────────────────────────────────────────────
    @Named("mapPoisFr")
    default List<String> mapPoisFr(Set<BienPoi> pois) {
        if (pois == null) return List.of();
        return pois.stream()
                .filter(p -> Boolean.TRUE.equals(p.getPresent()))  // seulement les POI trouvés
                .map(p -> p.getPoiType().getLabelFr())             // "École", "Pharmacie"...
                .sorted()                                           // ordre alphabétique
                .toList();
    }
}