package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.BienPublicDTO;
import com.kupanga.api.immobilier.dto.readDTO.ProprietairePublicDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BienMapper {

    @Mapping(target = "latitude",         expression = "java(bien.getLocalisation() != null ? bien.getLocalisation().getY() : null)")
    @Mapping(target = "longitude",        expression = "java(bien.getLocalisation() != null ? bien.getLocalisation().getX() : null)")
    @Mapping(target = "proprietaire",     source = "proprietaire", qualifiedByName = "mapProprietairePublic")
    @Mapping(target = "images",           source = "images",       qualifiedByName = "imageUrls")
    @Mapping(target = "pois",         source = "pois",         qualifiedByName = "mapPoisFr")
    // Locataire, contrats, quittances et documents n'existent pas dans BienPublicDTO (P0-6)
    BienPublicDTO toPublicDTO(Bien bien);

    // ─── Propriétaire public : prénom + initiale du nom + photo (jamais l'e-mail) ──
    @Named("mapProprietairePublic")
    default ProprietairePublicDTO mapProprietairePublic(User user) {
        if (user == null) return null;
        String nom = user.getLastName();
        return ProprietairePublicDTO.builder()
                .firstName(user.getFirstName())
                .initialeNom(nom == null || nom.isBlank() ? null : nom.trim().substring(0, 1).toUpperCase() + ".")
                .urlProfile(user.getUrlProfile())
                .build();
    }

    // ─── Images ───────────────────────────────────────────────────────────────
    @Named("imageUrls")
    default List<String> mapImages(Set<BienImage> images) {
        if (images == null) return List.of();
        return images.stream().map(BienImage::getUrl).toList();
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