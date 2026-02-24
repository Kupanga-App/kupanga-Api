package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienResponseDTO;
import com.kupanga.api.immobilier.dto.readDTO.LocataireSimpleDTO;
import com.kupanga.api.immobilier.dto.readDTO.ProprietaireSimpleDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BienMapper {

    @Mapping(target = "disponible", expression = "java(bien.getLocataire() == null)")
    @Mapping(source = "proprietaire", target = "proprietaire")
    @Mapping(source = "locataire", target = "locataire")
    BienResponseDTO toResponseDTO(Bien bien);

    @Mapping(target = "nom", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    ProprietaireSimpleDTO toProprietaireDTO(User user);

    @Mapping(target = "nom", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    LocataireSimpleDTO toLocataireDTO(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proprietaire", ignore = true)
    @Mapping(target = "locataire", ignore = true)
    @Mapping(target = "contrats", ignore = true)
    @Mapping(target = "quittances", ignore = true)
    @Mapping(target = "etatsDesLieux", ignore = true)
    @Mapping(target = "documents", ignore = true)
    Bien toEntity(BienFormDTO dto);
}
