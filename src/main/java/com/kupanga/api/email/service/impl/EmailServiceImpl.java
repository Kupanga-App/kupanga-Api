package com.kupanga.api.email.service.impl;

import com.kupanga.api.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.kupanga.api.email.constantes.Constante.*;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendWelcomeMessage(String destinataire, String prenom) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);

            // Sujet avec prénom
            helper.setSubject(String.format(
                    SUJET_MAIL_BIENVENUE_PROFIL_COMPLETE,
                    prenom
            ));

            // Contenu HTML avec prénom (présent deux fois)
            helper.setText(String.format(
                    CONTENU_MAIL_BIENVENUE_PROFIL_COMPLETE,
                    prenom, // pour le <h2>
                    prenom  // si tu ajoutes d’autres occurrences plus tard
            ), true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de bienvenue", e);
        }
    }


    @Override
    @Async
    public void sendPasswordResetMail(String destinataire, String resetLink) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject(SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE);

            String htmlContent =
                    String.format(CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE, resetLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(
                    "Erreur lors de l'envoi de l'email de réinitialisation", e);
        }
    }

    @Override
    @Async
    public void sendPasswordUpdatedConfirmation(String destinataire) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject(SUJET_MAIL_CONFIRMATION_MOT_DE_PASSE);
            helper.setText(CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(
                    "Erreur lors de l'envoi de l'email de confirmation du mot de passe", e);
        }
    }
}
