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
        String  urlProfile,
        boolean anonymise
) {
    /** @param urlProfile URL de la photo (S2 : présignée, construite par {@code PhotoProfilUrlMapper}) */
    public static UserAdminDTO from(User user, String urlProfile) {
        return new UserAdminDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getMail(),
                user.getRole(),
                user.getHasCompleteProfil(),
                urlProfile,
                user.isAnonymise()
        );
    }
}
