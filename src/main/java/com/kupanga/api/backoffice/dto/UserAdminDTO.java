package com.kupanga.api.backoffice.dto;

import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;

public record UserAdminDTO(
        Long    id,
        String  firstName,
        String  lastName,
        String  mail,
        Role    role,
        Boolean hasCompleteProfil,
        String  urlProfile
) {
    public static UserAdminDTO from(User user) {
        return new UserAdminDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getMail(),
                user.getRole(),
                user.getHasCompleteProfil(),
                user.getUrlProfile()
        );
    }
}
