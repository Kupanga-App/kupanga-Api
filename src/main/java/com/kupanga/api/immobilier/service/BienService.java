package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienPublicDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.user.entity.User;
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
     * Mise à jour partielle d'un bien (PATCH).
     * Seuls les champs non-null du DTO sont appliqués.
     * Une description vide ("") efface la valeur existante.
     * @param auth utilisateur connecté (doit être le propriétaire du bien)
     * @param bienId id du bien à modifier
     * @param dto champs à mettre à jour
     * @return le bien mis à jour
     */
    BienDTO updateBien(Authentication auth, Long bienId, BienUpdateDTO dto);

    /**
     * Récupère les infos du bien immobilier.
     * @param id id du bien.
     * @return le bien avec toutes ses infos.
     */
    BienPublicDTO getBienInfos(Long id);

    /**
     * Vue privée et détaillée d'un bien, réservée à son propriétaire et à son locataire.
     * @param bienId id du bien
     * @param email e-mail de l'utilisateur connecté
     * @return le bien (documents filtrés selon l'utilisateur)
     * @throws com.kupanga.api.exception.business.KupangaBusinessException 404 si absent, 403 sinon
     */
    BienDTO getBienPrive(Long bienId, String email);

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
     * Verifie si un bien appartient au propriétaire connecté ou pas
     * @param id id du bien
     * @param proprietaireId id du proprio
     * @return true or false
     */
    boolean existsByIdAndProprietaireId(Long id, Long proprietaireId);

    /**
     * Affecter un locataire à un bien.
     * @param auth pour vérifier les accès
     * @param userId id du locataire
     * @param bienId id du bien
     */
    void affectLocataire( Authentication auth , Long bienId , Long userId);

    /**
     * Charge le bien (avec ses relations) et vérifie qu'il appartient à l'utilisateur connecté.
     * À appeler avant toute action du propriétaire sur un bien (contrôle IDOR).
     * @param bienId id du bien
     * @param emailProprietaire e-mail de l'utilisateur connecté
     * @return le bien
     * @throws com.kupanga.api.exception.business.KupangaBusinessException 404 si le bien n'existe pas,
     *         403 s'il appartient à un autre propriétaire
     */
    Bien verifierProprietaire(Long bienId, String emailProprietaire);

    /**
     * Vérifie que l'e-mail fourni est celui du locataire actuellement assigné au bien.
     * @param bien le bien (déjà vérifié avec {@link #verifierProprietaire})
     * @param emailLocataire e-mail du locataire saisi par le propriétaire
     * @return le locataire du bien
     * @throws com.kupanga.api.exception.business.KupangaBusinessException 400 sinon
     */
    User verifierLocataireDuBien(Bien bien, String emailLocataire);
}
