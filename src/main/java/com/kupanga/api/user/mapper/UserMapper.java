package com.kupanga.api.user.mapper;

import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);
}

