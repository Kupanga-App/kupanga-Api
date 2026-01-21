package com.kupanga.api.utilisateur.mapper;

import com.kupanga.api.utilisateur.dto.formDTO.UtilisateurFormDTO;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {

    @Named("toDTO")
    UtilisateurDTO toDTO(Utilisateur utilisateur);

    @Named("toEntity")
    Utilisateur toEntity(UtilisateurFormDTO utilisateurFormDTO);
}
