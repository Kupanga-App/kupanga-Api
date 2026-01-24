package com.kupanga.api.utilisateur.mapper;

import com.kupanga.api.utilisateur.dto.readDTO.UserDTO;
import com.kupanga.api.utilisateur.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "mail", target = "mail")
    @Mapping(source = "password", target = "password")
    @Mapping(source = "role", target = "role")
    UserDTO toDTO(User user);
}

