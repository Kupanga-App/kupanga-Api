package com.kupanga.api.backoffice.dto;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.TypeBien;

import java.time.LocalDateTime;
import java.util.List;

public record BienAdminDTO(
        Long          id,
        String        titre,
        String        ville,
        String        pays,
        TypeBien      typeBien,
        Double        loyerMensuel,
        String        proprietaireMail,
        LocalDateTime createdAt,
        List<String>  images
) {
    public static BienAdminDTO from(Bien bien) {
        List<String> urls = bien.getImages() == null
                ? List.of()
                : bien.getImages().stream().map(img -> img.getUrl()).toList();

        return new BienAdminDTO(
                bien.getId(),
                bien.getTitre(),
                bien.getVille(),
                bien.getPays(),
                bien.getTypeBien(),
                bien.getLoyerMensuel(),
                bien.getProprietaire() != null ? bien.getProprietaire().getMail() : "—",
                bien.getCreatedAt(),
                urls
        );
    }
}
