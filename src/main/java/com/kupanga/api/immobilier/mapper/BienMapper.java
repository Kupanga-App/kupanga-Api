package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.BienPublicDTO;
import com.kupanga.api.immobilier.dto.readDTO.ProprietairePublicDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.PhotoProfilUrlMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {PhotoBienUrlMapper.class, PhotoProfilUrlMapper.class})
public interface BienMapper {

    // C2 (revue) : position publique arrondie à ~100 m ; la position exacte (repère GPS = porte du logement,
    // domicile du locataire) reste réservée aux parties du bien (BienDTO)
    @Mapping(target = "latitude",         expression = "java(bien.getLocalisation() != null ? arrondiPublic(bien.getLocalisation().getY()) : null)")
    @Mapping(target = "longitude",        expression = "java(bien.getLocalisation() != null ? arrondiPublic(bien.getLocalisation().getX()) : null)")
    @Mapping(target = "precisionLocalisation", source = "precisionLocalisation", qualifiedByName = "precisionPublique")
    @Mapping(target = "proprietaire",     source = "proprietaire", qualifiedByName = "mapProprietairePublic")
    @Mapping(target = "images",           source = "images",       qualifiedByName = "imageUrls")
    @Mapping(target = "vignettes",        source = "images",       qualifiedByName = "vignetteUrls")
    @Mapping(target = "pois",         source = "pois",         qualifiedByName = "mapPoisFr")
    // Locataire, contrats, quittances et documents n'existent pas dans BienPublicDTO (P0-6)
    BienPublicDTO toPublicDTO(Bien bien);

    // ─── Propriétaire public : prénom + initiale du nom + photo (jamais l'e-mail) ──
    // S2 : photo de profil privée → URL présignée de 24 h (PhotoProfilUrlMapper)
    @Named("mapProprietairePublic")
    @Mapping(target = "initialeNom", source = "lastName", qualifiedByName = "initialeNom")
    @Mapping(target = "urlProfile",  source = "user",     qualifiedByName = "urlPhotoProfil")
    ProprietairePublicDTO mapProprietairePublic(User user);

    @Named("initialeNom")
    default String initialeNom(String nom) {
        return nom == null || nom.isBlank() ? null : nom.trim().substring(0, 1).toUpperCase() + ".";
    }

    // ─── C2 : position publique ───────────────────────────────────────────────
    /** 3 décimales : environ 110 m en latitude. Nommée : jamais appliquée d'office à d'autres champs Double. */
    @Named("arrondiPublic")
    default Double arrondiPublic(double coordonnee) {
        return Math.round(coordonnee * 1000d) / 1000d;
    }

    /** Une position exacte (repère, adresse) est arrondie en public : sa précision n'est pas annoncée. */
    @Named("precisionPublique")
    default PrecisionLocalisation precisionPublique(PrecisionLocalisation precision) {
        return precision == null || precision.estPrecise() ? null : precision;
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