package com.kupanga.api.email.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.immobilier.entity.Contrat;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;

import static com.kupanga.api.email.constantes.Constante.*;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String reset_link;
    private final String url_login;
    private final String app_url;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${app.reset-link}") String reset_link,
                            @Value("${app.url-login}") String url_login,
                            @Value("${app.url}") String app_url
    ) {
        this.mailSender = mailSender;
        this.reset_link = reset_link;
        this.url_login = url_login;
        this.app_url = app_url;
    }

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
    public void sendPasswordResetMail(String destinataire, String resetToken) {


        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject(SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE);

            String htmlContent =
                    String.format(CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE, reset_link + resetToken);

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
            String htmlContent =
                    String.format(CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE, url_login);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(
                    "Erreur lors de l'envoi de l'email de confirmation du mot de passe", e);
        }
    }

    @Override
    @Async
    public void envoyerInvitationSignature(Contrat contrat, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            String prenomLocataire = contrat.getLocataire().getFirstName()
                    + " " + contrat.getLocataire().getLastName();

            String prenomProprietaire = contrat.getProprietaire().getFirstName()
                    + " " + contrat.getProprietaire().getLastName();

            helper.setTo(contrat.getLocataire().getMail());
            helper.setSubject(SUJET_MAIL_INVITATION_SIGNATURE);

            String lienSignature = app_url + "contrats/signer/" + token;

            String htmlContent = String.format(
                    CONTENU_MAIL_INVITATION_SIGNATURE,
                    prenomLocataire,
                    prenomProprietaire,
                    contrat.getAdresseBien(),
                    contrat.getLoyerMensuel(),
                    contrat.getChargesMensuelles(),
                    contrat.getDepotGarantie(),
                    contrat.getDateDebut(),
                    contrat.getDureeBailMois(),
                    lienSignature
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(
                    "Erreur lors de l'envoi de l'email d'invitation à la signature", e);
        }
    }

    @Override
    @Async
    public void envoyerConfirmationContratSigne(Contrat contrat) {
        try {
            // ─── Email au propriétaire ─────────────────────────────────────────
            envoyerEmailConfirmation(
                    contrat,
                    contrat.getProprietaire().getMail(),
                    contrat.getProprietaire().getFirstName() + " " + contrat.getProprietaire().getLastName()
            );

            // ─── Email au locataire ────────────────────────────────────────────
            envoyerEmailConfirmation(
                    contrat,
                    contrat.getLocataire().getMail(),
                    contrat.getLocataire().getFirstName() + " " + contrat.getLocataire().getLastName()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erreur lors de l'envoi des emails de confirmation du contrat signé", e);
        }
    }


    /**
     * Helper privé — évite la duplication entre proprio et locataire
     * @param contrat le contrat
     * @param destinataire destinataire
     * @param prenomNom prenom et nom
     * @throws MessagingException en cas d'exception
     */
    private void envoyerEmailConfirmation(Contrat contrat,
                                          String destinataire,
                                          String prenomNom) throws MessagingException, MalformedURLException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(destinataire);
        helper.setSubject(SUJET_MAIL_CONTRAT_SIGNE);

        String htmlContent = String.format(
                CONTENU_MAIL_CONTRAT_SIGNE,
                prenomNom,
                contrat.getAdresseBien(),
                contrat.getLoyerMensuel(),
                contrat.getChargesMensuelles(),
                contrat.getDepotGarantie(),
                contrat.getDateDebut(),
                contrat.getDureeBailMois()
        );

        helper.setText(htmlContent, true);

        // ─── Pièce jointe — PDF du contrat signé ──────────────────────────────
        if (contrat.getUrlPdf() != null) {
            helper.addAttachment(
                    "Contrat_" + contrat.getId() + ".pdf",
                    new UrlResource(contrat.getUrlPdf())
            );
        }

        mailSender.send(message);
    }
}
