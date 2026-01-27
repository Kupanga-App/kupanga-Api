package com.kupanga.api.user.mapper;

import com.kupanga.api.user.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.user.entity.AvatarProfil;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AvatarProfilMapper {

    AvatarProfilDTO toDTO(AvatarProfil avatarProfil);

    List<AvatarProfilDTO> toDTOList(List<AvatarProfil> avatarProfilList);
}
