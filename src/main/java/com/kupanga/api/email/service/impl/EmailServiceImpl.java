package com.kupanga.api.email.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.Quittance;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;

import static com.kupanga.api.email.constantes.Constante.*;

@Service
@Slf4j
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

    // ─────────────────────────────────────────────────────────────────────────
    // Invitation à signer
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Async
    public void envoyerInvitationSignature(EtatDesLieux edl, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String prenomNomLocataire    = fullName(edl.getLocataire().getFirstName(),
                    edl.getLocataire().getLastName());
            String prenomNomProprietaire = fullName(edl.getProprietaire().getFirstName(),
                    edl.getProprietaire().getLastName());
            String adresse               = edl.getBien().getAdresse() + ", "
                    + edl.getBien().getCodePostal() + " "
                    + edl.getBien().getVille();
            String typeEdl               = edl.getType().name().equals("ENTREE")
                    ? "État des lieux d'entrée"
                    : "État des lieux de sortie";
            String dateRealisation       = edl.getDateRealisation().toString();
            String lienSignature         = app_url + "edl/signer/" + token;

            helper.setTo(edl.getLocataire().getMail());
            helper.setSubject(SUJET_MAIL_INVITATION_SIGNATURE_EDL);

            String htmlContent = String.format(
                    CONTENU_MAIL_INVITATION_SIGNATURE_EDL,
                    prenomNomLocataire,     // %1$s
                    prenomNomProprietaire,  // %2$s
                    adresse,               // %3$s
                    typeEdl,               // %4$s
                    dateRealisation,       // %5$s
                    lienSignature          // %6$s
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Email invitation signature EDL {} envoyé à {}",
                    edl.getId(), edl.getLocataire().getMail());

        } catch (MessagingException e) {
            log.error("Erreur envoi email invitation signature EDL {} : {}",
                    edl.getId(), e.getMessage());
            throw new RuntimeException(
                    "Erreur lors de l'envoi de l'email d'invitation à la signature EDL", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Confirmation EDL signé (bailleur + locataire)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Async
    public void envoyerConfirmationEdlSigne(EtatDesLieux edl) {
        try {
            // Email au propriétaire
            envoyerEmailConfirmation(edl,
                    edl.getProprietaire().getMail(),
                    fullName(edl.getProprietaire().getFirstName(),
                            edl.getProprietaire().getLastName()));

            // Email au locataire
            envoyerEmailConfirmation(edl,
                    edl.getLocataire().getMail(),
                    fullName(edl.getLocataire().getFirstName(),
                            edl.getLocataire().getLastName()));

        } catch (Exception e) {
            log.error("Erreur envoi emails confirmation EDL signé {} : {}",
                    edl.getId(), e.getMessage());
            throw new RuntimeException(
                    "Erreur lors de l'envoi des emails de confirmation EDL signé", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Quittance
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Async
    public void envoyerQuittance(Quittance quittance) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = getMimeMessageHelper(quittance, message);

            // ─── Pièce jointe — PDF de la quittance ──────────────────────────
            if (quittance.getUrlPdf() != null) {
                String nomFichier = String.format("Quittance_%d_%d_%s.pdf",
                        quittance.getId(), quittance.getAnnee(), quittance.getMois());
                helper.addAttachment(nomFichier, new UrlResource(quittance.getUrlPdf()));
            }

            mailSender.send(message);
            log.info("Email quittance {} envoyé à {}",
                    quittance.getId(), quittance.getLocataire().getMail());

        } catch (MessagingException | MalformedURLException e) {
            log.error("Erreur envoi email quittance {} : {}", quittance.getId(), e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email de quittance", e);
        }
    }

    @NotNull
    private MimeMessageHelper getMimeMessageHelper(Quittance quittance, MimeMessage message) throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String moisLabel = (quittance.getMois())
                + " " + quittance.getAnnee();
        String adresse   = quittance.getBien().getAdresse() + ", "
                + quittance.getBien().getCodePostal() + " "
                + quittance.getBien().getVille();

        helper.setTo(quittance.getLocataire().getMail());
        helper.setSubject(SUJET_MAIL_QUITTANCE);
        helper.setText(String.format(
                CONTENU_MAIL_QUITTANCE,
                fullName(quittance.getLocataire().getFirstName(),    // %1$s
                        quittance.getLocataire().getLastName()),
                moisLabel,                                            // %2$s
                adresse,                                              // %3$s
                quittance.getLoyerMensuel(),                         // %4$s
                quittance.getChargesMensuelles(),                    // %5$s
                quittance.getMontantTotal(),                         // %6$s
                quittance.getDatePaiement() != null                  // %7$s
                        ? quittance.getDatePaiement().toString()
                        : "—"
        ), true);
        return helper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie l'email de confirmation à un destinataire avec le PDF en pièce jointe.
     */
    private void envoyerEmailConfirmation(EtatDesLieux edl,
                                          String destinataire,
                                          String prenomNom)
            throws MessagingException, MalformedURLException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String adresse = edl.getBien().getAdresse() + ", "
                + edl.getBien().getCodePostal() + " "
                + edl.getBien().getVille();
        String typeEdl = edl.getType().name().equals("ENTREE")
                ? "État des lieux d'entrée"
                : "État des lieux de sortie";

        helper.setTo(destinataire);
        helper.setSubject(SUJET_MAIL_EDL_SIGNE);

        String htmlContent = String.format(
                CONTENU_MAIL_EDL_SIGNE,
                prenomNom,              // %1$s
                adresse,               // %2$s
                typeEdl,               // %3$s
                edl.getDateRealisation().toString() // %4$s
        );

        helper.setText(htmlContent, true);

        // ─── Pièce jointe — PDF de l'EDL signé ───────────────────────────────
        if (edl.getUrlPdf() != null) {
            String nomFichier = String.format("EDL_%s_%d.pdf",
                    edl.getType().name(), edl.getId());
            helper.addAttachment(nomFichier, new UrlResource(edl.getUrlPdf()));
        }

        mailSender.send(message);

        log.info("Email confirmation EDL signé {} envoyé à {}", edl.getId(), destinataire);
    }

    /**
     * Concatène prénom et nom proprement.
     */
    private String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
}
