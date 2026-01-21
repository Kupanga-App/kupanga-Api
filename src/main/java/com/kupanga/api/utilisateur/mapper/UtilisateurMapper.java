package com.kupanga.api.utilisateur.mapper;

import com.kupanga.api.utilisateur.dto.formDTO.UtilisateurFormDTO;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {

    UtilisateurDTO toDTO(Utilisateur utilisateur);

    Utilisateur toEntity(UtilisateurFormDTO utilisateurFormDTO);
}
