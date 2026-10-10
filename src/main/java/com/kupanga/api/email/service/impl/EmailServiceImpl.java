package com.kupanga.api.email.service.impl;

import com.kupanga.api.juridiction.FormatMontant;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.email.dto.BrevoEmail;
import com.kupanga.api.email.dto.BrevoEmail.Recipient;
import com.kupanga.api.email.dto.BrevoEmail.Sender;
import com.kupanga.api.email.event.EmailAEnvoyer;
import com.kupanga.api.email.event.EmailAEnvoyer.PdfAJoindre;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.Quittance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

import static com.kupanga.api.email.constantes.Constante.*;
import static com.kupanga.api.minio.constant.MinioConstant.CONTRAT_BUCKET;
import static com.kupanga.api.minio.constant.MinioConstant.EDL_BUCKET;
import static com.kupanga.api.minio.constant.MinioConstant.QUITTANCE_BUCKET;

/**
 * Prépare les e-mails (contenu HTML échappé) dans le thread et la transaction de l'appelant, puis publie
 * un {@link EmailAEnvoyer}. B11 : l'envoi lui-même ({@code EmailEnvoiListener}) n'a lieu qu'après le commit.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final ApplicationEventPublisher evenements;
    private final Sender sender;
    private final String resetLink;
    private final String urlLogin;
    private final String appUrl;
    private final JuridictionRegistry juridictionRegistry;

    public EmailServiceImpl(
            ApplicationEventPublisher evenements,
            @Value("${brevo.sender-email}") String senderEmail,
            @Value("${brevo.sender-name}") String senderName,
            @Value("${app.reset-link}") String resetLink,
            @Value("${app.url-login}") String urlLogin,
            @Value("${app.url}") String appUrl,
            JuridictionRegistry juridictionRegistry
    ) {
        this.evenements = evenements;
        this.sender = new Sender(senderName, senderEmail);
        this.resetLink = resetLink;
        this.urlLogin = urlLogin;
        this.appUrl = appUrl;
        this.juridictionRegistry = juridictionRegistry;
    }

    @Override
    public void sendWelcomeMessage(String destinataire, String prenom) {
        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                String.format(SUJET_MAIL_BIENVENUE_PROFIL_COMPLETE, prenom),
                String.format(CONTENU_MAIL_BIENVENUE_PROFIL_COMPLETE, echapper(prenom), echapper(prenom)),
                null
        ), null);
    }

    @Override
    public void envoyerVerificationEmail(String destinataire, String prenom, String token) {
        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_VERIFICATION_EMAIL,
                String.format(CONTENU_MAIL_VERIFICATION_EMAIL, echapper(prenom), appUrl + "auth/verifier-email?token=" + token),
                null
        ), null);
    }

    @Override
    public void envoyerTentativeInscription(String destinataire) {
        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_TENTATIVE_INSCRIPTION,
                String.format(CONTENU_MAIL_TENTATIVE_INSCRIPTION, appUrl + "auth/forgot"),
                null
        ), null);
    }

    @Override
    public void sendPasswordResetMail(String destinataire, String resetToken) {
        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE,
                String.format(CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE, resetLink + resetToken),
                null
        ), null);
    }

    @Override
    public void sendPasswordUpdatedConfirmation(String destinataire) {
        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_CONFIRMATION_MOT_DE_PASSE,
                String.format(CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE, urlLogin),
                null
        ), null);
    }

    @Override
    public void envoyerInvitationSignature(Contrat contrat, String token) {
        String prenomLocataire = fullName(contrat.getLocataire().getFirstName(),
                contrat.getLocataire().getLastName());
        String prenomProprietaire = fullName(contrat.getProprietaire().getFirstName(),
                contrat.getProprietaire().getLastName());
        String lienSignature = appUrl + "contrats/signer/" + token;

        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(contrat.getLocataire().getMail())),
                SUJET_MAIL_INVITATION_SIGNATURE,
                String.format(CONTENU_MAIL_INVITATION_SIGNATURE,
                        prenomProprietaire,
                        prenomLocataire,
                        prenomProprietaire,
                        echapper(contrat.getAdresseBien()),
                        montants(contrat).f(contrat.getLoyerMensuel()),
                        montants(contrat).f(contrat.getChargesMensuelles()),
                        montants(contrat).f(contrat.getDepotGarantie()),
                        contrat.getDateDebut(),
                        contrat.getDureeBailMois(),
                        lienSignature),
                null
        ), null);
    }

    @Override
    public void envoyerConfirmationContratSigne(Contrat contrat) {
        envoyerConfirmationContrat(contrat, contrat.getProprietaire().getMail(),
                fullName(contrat.getProprietaire().getFirstName(), contrat.getProprietaire().getLastName()));
        envoyerConfirmationContrat(contrat, contrat.getLocataire().getMail(),
                fullName(contrat.getLocataire().getFirstName(), contrat.getLocataire().getLastName()));
    }

    @Override
    public void envoyerInvitationSignature(EtatDesLieux edl, String token) {
        String prenomNomLocataire = fullName(edl.getLocataire().getFirstName(),
                edl.getLocataire().getLastName());
        String prenomNomProprietaire = fullName(edl.getProprietaire().getFirstName(),
                edl.getProprietaire().getLastName());
        String adresse = buildAdresse(edl.getBien());
        String typeEdl = resolveTypeEdl(edl);
        String lienSignature = appUrl + "edl/signer/" + token;

        publier(new BrevoEmail(
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
        ), null);
        log.debug("E-mail d'invitation à signer l'EDL {} préparé", edl.getId());
    }

    @Override
    public void envoyerConfirmationEdlSigne(EtatDesLieux edl) {
        envoyerConfirmationEdl(edl, edl.getProprietaire().getMail(),
                fullName(edl.getProprietaire().getFirstName(), edl.getProprietaire().getLastName()));
        envoyerConfirmationEdl(edl, edl.getLocataire().getMail(),
                fullName(edl.getLocataire().getFirstName(), edl.getLocataire().getLastName()));
    }

    @Override
    public void envoyerQuittance(Quittance quittance) {
        String moisLabel = echapper(quittance.getMois() + " " + quittance.getAnnee());
        String adresse = buildAdresse(quittance.getBien());

        PdfAJoindre pdf = pdfAJoindre(QUITTANCE_BUCKET, quittance.getClePdf(),
                String.format("Quittance_%d_%d_%s.pdf", quittance.getId(), quittance.getAnnee(), quittance.getMois()));

        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(quittance.getLocataire().getMail())),
                SUJET_MAIL_QUITTANCE,
                String.format(CONTENU_MAIL_QUITTANCE,
                        fullName(quittance.getLocataire().getFirstName(),
                                quittance.getLocataire().getLastName()),
                        moisLabel,
                        adresse,
                        montants(quittance).f(quittance.getLoyerMensuel()),
                        montants(quittance).f(quittance.getChargesMensuelles()),
                        montants(quittance).f(quittance.getMontantTotal()),
                        quittance.getDatePaiement() != null
                                ? quittance.getDatePaiement().toString() : "—"),
                null
        ), pdf);
        log.debug("E-mail de la quittance {} préparé", quittance.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    /** J3 : montants dans la devise figée du document, formatés selon la langue de son pays. */
    private FormatMontant montants(Contrat contrat) {
        return juridictionRegistry.formatMontant(contrat.getPays(), contrat.getDevise());
    }

    private FormatMontant montants(Quittance quittance) {
        return juridictionRegistry.formatMontant(quittance.getPays(), quittance.getDevise());
    }

    private void envoyerConfirmationContrat(Contrat contrat, String destinataire, String prenomNom) {
        PdfAJoindre pdf = pdfAJoindre(CONTRAT_BUCKET, contrat.getClePdf(),
                "Contrat_" + contrat.getId() + ".pdf");

        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_CONTRAT_SIGNE,
                String.format(CONTENU_MAIL_CONTRAT_SIGNE,
                        prenomNom,
                        echapper(contrat.getAdresseBien()),
                        montants(contrat).f(contrat.getLoyerMensuel()),
                        montants(contrat).f(contrat.getChargesMensuelles()),
                        montants(contrat).f(contrat.getDepotGarantie()),
                        contrat.getDateDebut(),
                        contrat.getDureeBailMois()),
                null
        ), pdf);
    }

    private void envoyerConfirmationEdl(EtatDesLieux edl, String destinataire, String prenomNom) {
        String adresse = buildAdresse(edl.getBien());
        String typeEdl = resolveTypeEdl(edl);

        PdfAJoindre pdf = pdfAJoindre(EDL_BUCKET, edl.getClePdf(),
                String.format("EDL_%s_%d.pdf", edl.getType().name(), edl.getId()));

        publier(new BrevoEmail(
                sender,
                List.of(new Recipient(destinataire)),
                SUJET_MAIL_EDL_SIGNE,
                String.format(CONTENU_MAIL_EDL_SIGNE,
                        prenomNom,
                        adresse,
                        typeEdl,
                        edl.getDateRealisation().toString()),
                null
        ), pdf);
        log.debug("E-mail de confirmation de l'EDL signé {} préparé", edl.getId());
    }

    // Les helpers ci-dessous produisent du texte inséré dans le HTML des e-mails :
    // toute valeur saisie par un utilisateur y est échappée (pas d'injection HTML).

    private String fullName(String firstName, String lastName) {
        return echapper(firstName + " " + lastName);
    }

    /** J4 : adresse sur une ligne, sans « null » pour les champs vides (code postal en RDC…). */
    private String buildAdresse(Bien bien) {
        return echapper(bien.adresseComplete());
    }

    private String echapper(String valeur) {
        return valeur == null ? null : HtmlUtils.htmlEscape(valeur);
    }

    private String resolveTypeEdl(EtatDesLieux edl) {
        return edl.getType().name().equals("ENTREE")
                ? "État des lieux d'entrée"
                : "État des lieux de sortie";
    }

    /** PDF à joindre, lu dans son bucket privé au moment de l'envoi (P0-7) ; aucun si le document n'a pas de PDF. */
    private PdfAJoindre pdfAJoindre(String bucket, String clePdf, String nomFichier) {
        return clePdf == null ? null : new PdfAJoindre(bucket, clePdf, nomFichier);
    }

    /** B11 : publie l'e-mail ; il part après le commit de la transaction en cours (rien en cas d'annulation). */
    private void publier(BrevoEmail email, PdfAJoindre pdf) {
        evenements.publishEvent(new EmailAEnvoyer(email, pdf == null ? List.of() : List.of(pdf)));
    }
}
