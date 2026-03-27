package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.Bien;
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
     * @return le bien avec toutes ses infos.
     */
    BienDTO getBienInfos(Long id);

    /**
     * Retourne un bien avec toutes ses propriétés.
     * @param id id du bien
     * @return Bien .
     */
    Bien findWithAllProperties(Long id);

    /**
     * Rétourne tous les biens d'un utilisateur
     * @param email email de l'utilisateur
     * @return liste des biens du propriétaire
     */
    List<BienDTO> findAllPropertiesAssociateToUser(String email);

    /**
     * Trouve un bien grâce à son id.
     * @param bienId id du bien.
     * @return le bien.
     */
    Bien findById(Long bienId);

    /**
     * Verifie si un boien appartient au propriétaire connecté ou pas
     * @param id id du bien
     * @param proprietaireId id du proprio
     * @return true or false
     */
    boolean existsByIdAndProprietaireId(Long id, Long proprietaireId);
}
