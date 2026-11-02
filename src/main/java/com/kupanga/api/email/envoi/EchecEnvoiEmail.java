package com.kupanga.api.email.envoi;

/**
 * M1 : envoi refusé ou impossible. Le message ne contient ni adresse, ni sujet, ni contenu (W10) : seulement la
 * classe de l'erreur et le code SMTP.
 */
public class EchecEnvoiEmail extends Exception {

    private final boolean definitif;
    private final Integer codeSmtp;

    /**
     * @param definitif {@code true} : inutile de réessayer (adresse refusée par le serveur, message invalide) ;
     *                  {@code false} : erreur passagère (réseau, serveur indisponible, 4xx SMTP, identifiants)
     * @param codeSmtp  code de réponse SMTP s'il est connu
     */
    public EchecEnvoiEmail(String message, boolean definitif, Integer codeSmtp, Throwable cause) {
        super(message, cause);
        this.definitif = definitif;
        this.codeSmtp = codeSmtp;
    }

    public boolean isDefinitif() {
        return definitif;
    }

    public Integer getCodeSmtp() {
        return codeSmtp;
    }
}
