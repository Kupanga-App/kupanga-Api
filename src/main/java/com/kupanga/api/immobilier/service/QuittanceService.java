package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.formDTO.MarquerPayeeDTO;
import org.springframework.web.multipart.MultipartFile;
import com.kupanga.api.immobilier.dto.formDTO.QuittanceFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;

import java.util.List;

public interface QuittanceService {

    /**
     * Crée une quittance et génère le PDF.
     * Si un contratId est fourni, loyer et charges sont récupérés depuis le contrat.
     *
     * @param dto               données de la quittance
     * @param emailProprietaire email du propriétaire connecté
     */
    void creerQuittance(QuittanceFormDTO dto, String emailProprietaire);

    /**
     * Marque la quittance comme payée, enregistre la signature et la date de paiement,
     * régénère le PDF et envoie la quittance par email au locataire.
     *
     * @param quittanceId       id de la quittance
     * @param dto               C8-bis : signature, mode, date et référence du paiement reçu hors application
     * @param emailProprietaire email du propriétaire connecté
     */
    void marquerPayee(Long quittanceId, MarquerPayeeDTO dto, String emailProprietaire);

    /**
     * C8-bis : joint (ou remplace) la photo du reçu d'une quittance payée, dans un bucket privé.
     * 404 / 403 / 409 (non payée, bien archivé, compte supprimé) / 400, 413, 415 (fichier).
     */
    QuittanceDTO ajouterRecu(Long quittanceId, MultipartFile recu, String emailProprietaire);

    /**
     * Récupère toutes les quittances d'un bien.
     *
     * @param bienId            id du bien
     * @param emailProprietaire email du propriétaire connecté
     * @return liste des quittances
     */
    List<QuittanceDTO> getQuittancesParBien(Long bienId, String emailProprietaire);

    /**
     * Récupère toutes les quittances du locataire connecté.
     *
     * @param emailLocataire email du locataire connecté
     * @return liste des quittances
     */
    List<QuittanceDTO> getQuittancesParLocataire(String emailLocataire);

    /**
     * Récupère une quittance par son id.
     *
     * @param quittanceId       id de la quittance
     * @param emailUtilisateur  email de l'utilisateur connecté (proprio ou locataire)
     * @return la quittance
     */
    QuittanceDTO getQuittanceById(Long quittanceId, String emailUtilisateur);
}