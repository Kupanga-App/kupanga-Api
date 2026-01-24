package com.kupanga.api.utilisateur.mapper;

import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.utilisateur.entity.AvatarProfil;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AvatarProfilMapper {

    AvatarProfilDTO toDTO(AvatarProfil avatarProfil);
}
