package com.kupanga.api.authentification.constant;

public class AuthConstant {

    public static final String DECONNEXION = " Déconnexion Réussie " ;
    public static final String REFRESHTOKEN ="refreshToken";
    public static final String AUTHORIZATION  ="Authorization";
    public static final String BEARER ="Bearer ";
    public static final String MOT_DE_PASSE_A_JOUR = " Votre mot de passe est à jour";
    public static final String MAIL_REINITIALISATION_ENVOYE =
            "Si un compte existe pour cet e-mail, un lien de réinitialisation vient d'être envoyé";
    public static final String TOKEN_REINITIALISATION_INVALIDE = "Lien de réinitialisation invalide ou expiré";

    // A14 : vérification de l'adresse e-mail
    public static final String COMPTE_CREE_VERIFIER_EMAIL =
            "Compte créé. Un lien de confirmation vient d'être envoyé à votre adresse e-mail : "
                    + "ouvrez-le pour activer votre compte (valable 24 heures).";
    public static final String EMAIL_NON_VERIFIE =
            "Votre adresse e-mail n'est pas encore confirmée. Ouvrez le lien reçu par e-mail ou demandez-en un nouveau.";
    public static final String EMAIL_VERIFIE = "Adresse e-mail confirmée : vous pouvez vous connecter.";
    /** A4 : jeton Google dont l'adresse n'est pas confirmée par Google. */
    public static final String ADRESSE_GOOGLE_NON_VERIFIEE =
            "Votre adresse e-mail Google n'est pas confirmée : confirmez-la auprès de Google ou inscrivez-vous avec un mot de passe";
    public static final String LIEN_VERIFICATION_INVALIDE = "Lien de confirmation invalide ou expiré";
    public static final String LIEN_VERIFICATION_ENVOYE =
            "Si un compte non confirmé existe pour cet e-mail, un nouveau lien de confirmation vient d'être envoyé";
}
