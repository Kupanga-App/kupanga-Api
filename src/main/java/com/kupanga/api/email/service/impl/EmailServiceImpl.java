package com.kupanga.api.email.service.impl;

import com.kupanga.api.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import static com.kupanga.api.email.constantes.Constante.CONTENU_MAIL_MOT_DE_PASSE_TEMPORAIRE;
import static com.kupanga.api.email.constantes.Constante.SUJET_MAIL_MOT_DE_PASSE_TEMPORAIRE;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void envoyerMailMotDePasseTemporaire(String destinataire, String password){

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject(SUJET_MAIL_MOT_DE_PASSE_TEMPORAIRE);

            String htmlContent = String.format(CONTENU_MAIL_MOT_DE_PASSE_TEMPORAIRE, password);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    @Override
    public void envoyerMailNouveauCompteFinaliser(String destinataire, String nom, String prenom, String email){

    }
}
