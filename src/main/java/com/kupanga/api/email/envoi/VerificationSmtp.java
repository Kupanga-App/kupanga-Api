package com.kupanga.api.email.envoi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * M4 : au démarrage, vérifie qu'on peut se connecter et s'authentifier au serveur SMTP. Un échec est journalisé en
 * ERROR (→ Sentry) sans bloquer le démarrage : les e-mails attendent en file et repartiront une fois la
 * configuration corrigée. Fait dans un autre thread (le serveur peut mettre jusqu'à 10 s à répondre).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "email.envoi.actif", havingValue = "true", matchIfMissing = true)
public class VerificationSmtp {

    private final JavaMailSender mailSender;

    @Async("emailExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void auDemarrage() {
        verifier();
    }

    /** @return {@code true} si la connexion et l'authentification SMTP réussissent */
    public boolean verifier() {
        if (!(mailSender instanceof JavaMailSenderImpl smtp)) {
            return true;
        }
        try {
            smtp.testConnection();
            log.info("Serveur SMTP joignable ({}:{})", smtp.getHost(), smtp.getPort());
            return true;
        } catch (Exception e) {
            // Ni identifiant ni mot de passe : hôte, port et classe de l'erreur seulement
            log.error("Serveur SMTP injoignable ou identifiants refusés ({}:{}) : {} — les e-mails restent en file",
                    smtp.getHost(), smtp.getPort(), racine(e).getClass().getSimpleName());
            return false;
        }
    }

    private static Throwable racine(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c;
    }
}
