package com.kupanga.api.utilisateur.mapper;

import com.kupanga.api.utilisateur.dto.formDTO.UtilisateurFormDTO;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {

    @Mapping(source = "email", target = "email")
    @Mapping(source = "motDePasse", target = "motDePasse")
    @Mapping(source = "role", target = "role")
    UtilisateurDTO toDTO(Utilisateur utilisateur);

    @Mapping(source = "email", target = "email")
    @Mapping(source = "role", target = "role")
    Utilisateur toEntity(UtilisateurFormDTO utilisateurFormDTO);
}
