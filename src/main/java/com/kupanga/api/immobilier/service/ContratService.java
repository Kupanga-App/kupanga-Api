package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.formDTO.ContratFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;

public interface ContratService {

    /**
     * Créer le contrat et génère le PDF
     * @param dto le dto
     * @param emailProprietaire emailPropriétaire
     */
    void creerContrat(ContratFormDTO dto, String emailProprietaire);

    /**
     * Signature du propriétaire
     * @param contratId id du contrat
     * @param signatureBase64 signature
     * @param emailProprietaire email propriétaire
     */
    void signerProprietaire(Long contratId, String signatureBase64, String emailProprietaire);

    /**
     * Signature du locataire
     * @param token token de signature
     * @param signatureBase64 signature.
     */
    void signerLocataire(String token, String signatureBase64);

    /**
     * Récupère les informations d'un contrat à partir du token de signature.
     * Utilisé par le locataire pour consulter le contrat avant de signer.
     *
     * @param token le token de signature reçu par email
     * @return le DTO du contrat
     */
    ContratDTO getContratParToken(String token);
}
