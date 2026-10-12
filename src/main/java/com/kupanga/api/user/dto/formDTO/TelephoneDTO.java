package com.kupanga.api.user.dto.formDTO;

import com.kupanga.api.user.validation.Telephone;

/** C6 : numéro de contact de l'utilisateur connecté ; {@code null} ou vide l'efface. */
public record TelephoneDTO(
        @Telephone
        String telephone
) {}
