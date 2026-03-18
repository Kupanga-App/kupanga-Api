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

@Mapper(componentModel = "spring" , uses = {UserMapper.class})
public interface BienMapper {


    @Mapping(target = "proprietaire", source = "proprietaire",qualifiedByName = "mapUserSansInfosSensibles")
    @Mapping(target = "locataire",    ignore = true)
    @Mapping(target = "contrats",     ignore = true)
    @Mapping(target = "quittances",   ignore = true)
    @Mapping(target = "documents",    ignore = true)
    @Mapping(target = "images",       ignore = true)
    BienDTO toDTO(Bien bien);

    // ─── Méthodes nommées ─────────────────────────────────────────────────────

    @Named("mapUserSansInfosSensibles")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "mail", ignore = true)
    UserDTO mapUserSansInfosSensibles(User user);

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

    @Named("imageUrls")
    default List<String> mapImages(Set<BienImage> images) {
        if (images == null) return List.of();
        return images.stream().map(BienImage::getUrl).toList();
    }
}
