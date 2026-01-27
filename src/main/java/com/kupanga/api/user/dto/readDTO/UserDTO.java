package com.kupanga.api.user.dto.readDTO;

import com.kupanga.api.user.entity.Role;
import lombok.Builder;

@Builder
public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String mail ,
        String password ,
        Role role
) {
}
