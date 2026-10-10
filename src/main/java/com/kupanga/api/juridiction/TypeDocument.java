package com.kupanga.api.juridiction;

/**
 * J5 : documents générés en PDF, chacun avec un modèle par pays et par version
 * ({@code templates/documents/<pays>/<modeleVersion>/<fichier>.html}).
 */
public enum TypeDocument {

    CONTRAT("contrat"),
    QUITTANCE("quittance"),
    ETAT_DES_LIEUX("etat-des-lieux");

    private final String fichier;

    TypeDocument(String fichier) {
        this.fichier = fichier;
    }

    /** Nom du fichier du modèle, sans extension. */
    public String getFichier() {
        return fichier;
    }
}
