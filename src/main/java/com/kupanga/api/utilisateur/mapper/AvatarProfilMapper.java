package com.kupanga.api.utilisateur.mapper;

import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.utilisateur.entity.AvatarProfil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AvatarProfilMapper {

    @Mapping(source = "id" , target = "id")
    @Mapping(source = "url" , target = "url")
    AvatarProfilDTO toDTO(AvatarProfil avatarProfil);
}
