package com.kupanga.api.user.mapper;

import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role",     ignore = true)
    UserDTO toDTOWithoutCredentials(User user);
}

