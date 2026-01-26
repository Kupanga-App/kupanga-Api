package com.kupanga.api.utilisateur.mapper;

import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.utilisateur.entity.AvatarProfil;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AvatarProfilMapper {

    AvatarProfilDTO toDTO(AvatarProfil avatarProfil);

    List<AvatarProfilDTO> toDTOList(List<AvatarProfil> avatarProfilList);
}
