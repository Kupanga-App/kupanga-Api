package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring" , uses = {UserMapper.class})
public interface BienMapper {


    @Mapping(target = "latitude",     expression = "java(bien.getLocalisation() != null ? bien.getLocalisation().getY() : null)")
    @Mapping(target = "longitude",    expression = "java(bien.getLocalisation() != null ? bien.getLocalisation().getX() : null)")
    @Mapping(target = "proprietaire", source = "proprietaire")
    @Mapping(target = "locataire",    source = "locataire")
    @Mapping(target = "contrats",     source = "contrats",   qualifiedByName = "contratUrls")
    @Mapping(target = "quittances",   source = "quittances", qualifiedByName = "quittanceUrls")
    @Mapping(target = "documents",    source = "documents",  qualifiedByName = "documentUrls")
    @Mapping(target = "images",       source = "images",     qualifiedByName = "imageUrls")
    BienDTO toDTO(Bien bien);

    // ─── Méthodes nommées ─────────────────────────────────────────────────────

    @Named("contratUrls")
    default List<String> mapContrats(List<Contrat> contrats) {
        if (contrats == null) return List.of();
        return contrats.stream().map(Contrat::getUrlPdf).toList();
    }

    @Named("quittanceUrls")
    default List<String> mapQuittances(List<Quittance> quittances) {
        if (quittances == null) return List.of();
        return quittances.stream().map(Quittance::getUrlPdf).toList();
    }

    @Named("documentUrls")
    default List<String> mapDocuments(List<Document> documents) {
        if (documents == null) return List.of();
        return documents.stream().map(Document::getUrl).toList();
    }

    @Named("imageUrls")
    default List<String> mapImages(List<BienImage> images) {
        if (images == null) return List.of();
        return images.stream().map(BienImage::getUrl).toList();
    }
}
