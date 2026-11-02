package com.kupanga.api.email.envoi;

import com.kupanga.api.email.dto.EmailSortant;

/**
 * M1 : envoie un e-mail. Implémentation par défaut : SMTP générique ({@link SmtpExpediteurEmail}), configurée par
 * {@code spring.mail.*} ; changer de fournisseur (Brevo, Scaleway, Mailjet, SES, relais de l'hébergeur…) ne demande
 * que de changer ces variables.
 */
public interface ExpediteurEmail {

    /**
     * @throws EchecEnvoiEmail si le serveur refuse le message ou n'est pas joignable
     */
    void envoyer(EmailSortant email) throws EchecEnvoiEmail;
}
