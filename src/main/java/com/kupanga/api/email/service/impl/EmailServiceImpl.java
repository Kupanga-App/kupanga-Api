package com.kupanga.api.email.service.impl;

import com.kupanga.api.email.client.BrevoEmailClient;
import com.kupanga.api.email.dto.BrevoEmail;
import com.kupanga.api.email.dto.BrevoEmail.Attachment;
import com.kupanga.api.email.dto.BrevoEmail.Recipient;
import com.kupanga.api.email.dto.BrevoEmail.Sender;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.Quittance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.kupanga.api.email.constantes.Constante.*;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final BrevoEmailClient brevoClient;
    private final Sender sender;
    private final String resetLink;
    private final String urlLogin;
    private final String appUrl;

    public EmailServiceImpl(
            BrevoEmailClient brevoClient,
            @Value("${brevo.sender-email}") String senderEmail,
            @Value("${brevo.sender-name}") String senderName,
            @Value("${app.reset-link}") String resetLink,
            @Value("${app.url-login}") String urlLogin,
            @Value("${app.url}") String appUrl
    ) {
        this.brevoClient = brevoClient;
        this.sender = new Sender(senderName, senderEmail);
        this.resetLink = resetLink;
        this.urlLogin = urlLogin;
        this.appUrl = appUrl;
    }

    @Override
    @Async
    public void sendWelcomeMessage(String destinataire, String prenom) {
        log.info(">>> SEND WELCOME EMAIL START pour l'utilisateur {}", destinataire);
        brevoClient.send(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                String.format(SUJET_MAIL_BIENVENUE_PROFIL_COMPLETE, prenom),
                String.format(CONTENU_MAIL_BIENVENUE_PROFIL_COMPLETE, prenom, prenom),
                null
        ));
    }

    @Override
    @Async
    public void sendPasswordResetMail(String destinataire, String resetToken) {
        brevoClient.send(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE,
                String.format(CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE, resetLink + resetToken),
                null
        ));
    }

    @Override
    @Async
    public void sendPasswordUpdatedConfirmation(String destinataire) {
        brevoClient.send(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_CONFIRMATION_MOT_DE_PASSE,
                String.format(CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE, urlLogin),
                null
        ));
    }

    @Override
    @Async
    public void envoyerInvitationSignature(Contrat contrat, String token) {
        String prenomLocataire = fullName(contrat.getLocataire().getFirstName(),
                contrat.getLocataire().getLastName());
        String prenomProprietaire = fullName(contrat.getProprietaire().getFirstName(),
                contrat.getProprietaire().getLastName());
        String lienSignature = appUrl + "contrats/signer/" + token;

        brevoClient.send(new BrevoEmail(
                sender,
                List.of(new Recipient(contrat.getLocataire().getMail())),
                SUJET_MAIL_INVITATION_SIGNATURE,
                String.format(CONTENU_MAIL_INVITATION_SIGNATURE,
                        prenomProprietaire,
                        prenomLocataire,
                        prenomProprietaire,
                        contrat.getAdresseBien(),
                        contrat.getLoyerMensuel(),
                        contrat.getChargesMensuelles(),
                        contrat.getDepotGarantie(),
                        contrat.getDateDebut(),
                        contrat.getDureeBailMois(),
                        lienSignature),
                null
        ));
    }

    @Override
    @Async
    public void envoyerConfirmationContratSigne(Contrat contrat) {
        envoyerConfirmationContrat(contrat, contrat.getProprietaire().getMail(),
                fullName(contrat.getProprietaire().getFirstName(), contrat.getProprietaire().getLastName()));
        envoyerConfirmationContrat(contrat, contrat.getLocataire().getMail(),
                fullName(contrat.getLocataire().getFirstName(), contrat.getLocataire().getLastName()));
    }

    @Override
    @Async
    public void envoyerInvitationSignature(EtatDesLieux edl, String token) {
        String prenomNomLocataire = fullName(edl.getLocataire().getFirstName(),
                edl.getLocataire().getLastName());
        String prenomNomProprietaire = fullName(edl.getProprietaire().getFirstName(),
                edl.getProprietaire().getLastName());
        String adresse = buildAdresse(edl.getBien().getAdresse(),
                edl.getBien().getCodePostal(), edl.getBien().getVille());
        String typeEdl = resolveTypeEdl(edl);
        String lienSignature = appUrl + "edl/signer/" + token;

        brevoClient.send(new BrevoEmail(
                sender,
                List.of(new Recipient(edl.getLocataire().getMail())),
                SUJET_MAIL_INVITATION_SIGNATURE_EDL,
                String.format(CONTENU_MAIL_INVITATION_SIGNATURE_EDL,
                        prenomNomLocataire,
                        prenomNomProprietaire,
                        adresse,
                        typeEdl,
                        edl.getDateRealisation().toString(),
                        lienSignature),
                null
        ));
        log.info("Email invitation signature EDL {} envoyé à {}", edl.getId(), edl.getLocataire().getMail());
    }

    @Override
    @Async
    public void envoyerConfirmationEdlSigne(EtatDesLieux edl) {
        envoyerConfirmationEdl(edl, edl.getProprietaire().getMail(),
                fullName(edl.getProprietaire().getFirstName(), edl.getProprietaire().getLastName()));
        envoyerConfirmationEdl(edl, edl.getLocataire().getMail(),
                fullName(edl.getLocataire().getFirstName(), edl.getLocataire().getLastName()));
    }

    @Override
    @Async
    public void envoyerQuittance(Quittance quittance) {
        String moisLabel = quittance.getMois() + " " + quittance.getAnnee();
        String adresse = buildAdresse(quittance.getBien().getAdresse(),
                quittance.getBien().getCodePostal(), quittance.getBien().getVille());

        List<Attachment> attachments = null;
        if (quittance.getUrlPdf() != null) {
            String nomFichier = String.format("Quittance_%d_%d_%s.pdf",
                    quittance.getId(), quittance.getAnnee(), quittance.getMois());
            attachments = List.of(new Attachment(quittance.getUrlPdf(), nomFichier));
        }

        brevoClient.send(new BrevoEmail(
                sender,
                List.of(new Recipient(quittance.getLocataire().getMail())),
                SUJET_MAIL_QUITTANCE,
                String.format(CONTENU_MAIL_QUITTANCE,
                        fullName(quittance.getLocataire().getFirstName(),
                                quittance.getLocataire().getLastName()),
                        moisLabel,
                        adresse,
                        quittance.getLoyerMensuel(),
                        quittance.getChargesMensuelles(),
                        quittance.getMontantTotal(),
                        quittance.getDatePaiement() != null
                                ? quittance.getDatePaiement().toString() : "—"),
                attachments
        ));
        log.info("Email quittance {} envoyé à {}", quittance.getId(), quittance.getLocataire().getMail());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private void envoyerConfirmationContrat(Contrat contrat, String destinataire, String prenomNom) {
        List<Attachment> attachments = null;
        if (contrat.getUrlPdf() != null) {
            attachments = List.of(new Attachment(contrat.getUrlPdf(),
                    "Contrat_" + contrat.getId() + ".pdf"));
        }

        brevoClient.send(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_CONTRAT_SIGNE,
                String.format(CONTENU_MAIL_CONTRAT_SIGNE,
                        prenomNom,
                        contrat.getAdresseBien(),
                        contrat.getLoyerMensuel(),
                        contrat.getChargesMensuelles(),
                        contrat.getDepotGarantie(),
                        contrat.getDateDebut(),
                        contrat.getDureeBailMois()),
                attachments
        ));
    }

    private void envoyerConfirmationEdl(EtatDesLieux edl, String destinataire, String prenomNom) {
        String adresse = buildAdresse(edl.getBien().getAdresse(),
                edl.getBien().getCodePostal(), edl.getBien().getVille());
        String typeEdl = resolveTypeEdl(edl);

        List<Attachment> attachments = null;
        if (edl.getUrlPdf() != null) {
            attachments = List.of(new Attachment(edl.getUrlPdf(),
                    String.format("EDL_%s_%d.pdf", edl.getType().name(), edl.getId())));
        }

        brevoClient.send(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_EDL_SIGNE,
                String.format(CONTENU_MAIL_EDL_SIGNE,
                        prenomNom,
                        adresse,
                        typeEdl,
                        edl.getDateRealisation().toString()),
                attachments
        ));
        log.info("Email confirmation EDL signé {} envoyé à {}", edl.getId(), destinataire);
    }

    private String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    private String buildAdresse(String adresse, String codePostal, String ville) {
        return adresse + ", " + codePostal + " " + ville;
    }

    private String resolveTypeEdl(EtatDesLieux edl) {
        return edl.getType().name().equals("ENTREE")
                ? "État des lieux d'entrée"
                : "État des lieux de sortie";
    }
}
