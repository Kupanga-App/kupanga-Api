package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BienService {

    /**
     * Création d'un bien immobilier.
     * @param auth pour récupérer le rôle de l'utilisateur
     * @param bienFormDTO formulaire de bien
     * @param files les photos du bien.
     */
    void createBien(Authentication auth , BienFormDTO bienFormDTO , List<MultipartFile> files);

    /**
     * Récupère les infos du bien immobilier.
     * @param id id du bien.
     * @param auth pour récupérer les crédentials.
     * @return le bien avec toutes ses infos.
     */
    BienDTO getBienInfos( Authentication auth , Long id);
}
