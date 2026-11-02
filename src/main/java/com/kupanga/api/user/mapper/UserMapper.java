package com.kupanga.api.user.mapper;

import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** S2 : photo de profil privée → URL présignée de 24 h ({@link PhotoProfilUrlMapper}). */
@Mapper(componentModel = "spring", uses = PhotoProfilUrlMapper.class)
public interface UserMapper {

    @Mapping(target = "urlProfile", source = "user", qualifiedByName = "urlPhotoProfil")
    UserDTO toDTO(User user);

    @Mapping(target = "urlProfile", source = "user", qualifiedByName = "urlPhotoProfil")
    @Mapping(target = "role",     ignore = true)
    @Mapping(target = "telephone", ignore = true) // C6 : jamais dans la recherche de candidats
    UserDTO toDTOWithoutCredentials(User user);
}

