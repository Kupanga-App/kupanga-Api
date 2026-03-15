package com.kupanga.api.immobilier.entity;

public enum StatutContrat {
    BROUILLON,                        // formulaire rempli, PDF pas encore généré
    EN_ATTENTE_SIGNATURE_PROPRIO,     // PDF généré, proprio doit signer
    EN_ATTENTE_SIGNATURE_LOCATAIRE,   // proprio a signé, locataire doit signer
    SIGNE,                            // les deux ont signé
    EXPIRE,                           // token de signature expiré
    ANNULE                            // contrat annulé
}