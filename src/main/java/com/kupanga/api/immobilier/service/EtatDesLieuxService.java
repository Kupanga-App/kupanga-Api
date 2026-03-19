package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.formDTO.EtatDesLieuxFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;

public interface EtatDesLieuxService {

    /**
     * Crée l'état des lieux et génère le PDF initial (sans signatures).
     *
     * @param dto               les données de l'EDL
     * @param emailProprietaire email du propriétaire connecté
     */
    void creerEtatDesLieux(EtatDesLieuxFormDTO dto, String emailProprietaire);

    /**
     * Signature du propriétaire — régénère le PDF et envoie l'invitation au locataire.
     *
     * @param edlId             id de l'EDL
     * @param signatureBase64   signature en base64
     * @param emailProprietaire email du propriétaire connecté
     */
    void signerProprietaire(Long edlId, String signatureBase64, String emailProprietaire);

    /**
     * Signature du locataire via token — génère le PDF final et envoie les confirmations.
     *
     * @param token           token de signature reçu par email
     * @param signatureBase64 signature en base64
     */
    void signerLocataire(String token, String signatureBase64);

    /**
     * Récupère les informations d'un EDL à partir du token de signature.
     * Utilisé par le locataire pour consulter l'EDL avant de signer.
     *
     * @param token le token de signature reçu par email
     * @return le DTO de l'EDL
     */
    EtatDesLieuxDTO getEdlParToken(String token);
}