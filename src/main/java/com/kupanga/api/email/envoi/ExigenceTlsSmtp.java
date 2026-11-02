package com.kupanga.api.email.envoi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Revue M2 : en production, la connexion au serveur SMTP doit être chiffrée (STARTTLS exigé sur le port 587, ou
 * SSL sur le port 465). Sinon le mot de passe SMTP et les e-mails (liens de connexion, de reset) circuleraient en
 * clair : l'application refuse de démarrer plutôt que d'envoyer en clair par erreur de configuration.
 */
@Component
@Profile("prod")
public class ExigenceTlsSmtp {

    public ExigenceTlsSmtp(
            @Value("${spring.mail.properties.mail.smtp.starttls.required:false}") boolean starttlsExige,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}") boolean ssl) {
        verifier(starttlsExige, ssl);
    }

    static void verifier(boolean starttlsExige, boolean ssl) {
        if (!starttlsExige && !ssl) {
            throw new IllegalStateException("SMTP sans chiffrement refusé en production : "
                    + "SMTP_STARTTLS=true (port 587) ou SMTP_SSL=true (port 465)");
        }
    }
}
