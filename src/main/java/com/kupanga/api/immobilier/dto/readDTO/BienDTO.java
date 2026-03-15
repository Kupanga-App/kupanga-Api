package com.kupanga.api.immobilier.dto.readDTO;


import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.user.dto.readDTO.UserDTO;

import java.time.LocalDateTime;
import java.util.List;

public record BienDTO(
        Long             id,
        String           titre,
        String           adresse,
        String           ville,
        String           codePostal,
        String           pays,
        Double           latitude,
        Double           longitude,
        TypeBien typeBien,
        String           description,
        LocalDateTime createdAt,
        LocalDateTime    updatedAt,
        UserDTO proprietaire,
        UserDTO locataire,
        List<String> contrats,
        List<String> quittances,
        List<String> documents,
        List<String> images
) {
}
