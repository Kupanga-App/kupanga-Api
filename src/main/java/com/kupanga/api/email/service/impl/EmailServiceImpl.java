package com.kupanga.api.email.service.impl;

import com.kupanga.api.user.entity.User;
import com.kupanga.api.immobilier.entity.StatutLocation;
import com.kupanga.api.immobilier.entity.Location;
import com.kupanga.api.juridiction.FormatMontant;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.email.dto.EmailSortant;
import com.kupanga.api.email.file.FileEmails;
import com.kupanga.api.email.file.PdfAJoindre;
import com.kupanga.api.email.file.TypeEmail;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.DemandeLocation;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.Quittance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.kupanga.api.email.constantes.Constante.*;
import static com.kupanga.api.stockage.constant.StockageConstant.CONTRAT_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.EDL_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.QUITTANCE_BUCKET;

/**
 * Prépare les e-mails (contenu HTML échappé) dans le thread et la transaction de l'appelant, puis les met en file
 * ({@link FileEmails}, M2). B11 : l'envoi lui-même ({@code EnvoyeurEmails}) n'a lieu qu'après le commit.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final FileEmails fileEmails;
    private final String resetLink;
    private final String urlLogin;
    private final String appUrl;
    private final JuridictionRegistry juridictionRegistry;

    public EmailServiceImpl(
            FileEmails fileEmails,
            @Value("${app.reset-link}") String resetLink,
            @Value("${app.url-login}") String urlLogin,
            @Value("${app.url}") String appUrl,
            JuridictionRegistry juridictionRegistry
    ) {
        this.fileEmails = fileEmails;
        this.resetLink = resetLink;
        this.urlLogin = urlLogin;
        this.appUrl = appUrl;
        this.juridictionRegistry = juridictionRegistry;
    }

    @Override
    public void sendWelcomeMessage(String destinataire, String prenom) {
        publier(TypeEmail.BIENVENUE, EmailSortant.html(
                destinataire,
                String.format(SUJET_MAIL_BIENVENUE_PROFIL_COMPLETE, prenom),
                String.format(CONTENU_MAIL_BIENVENUE_PROFIL_COMPLETE, echapper(prenom), echapper(prenom))
        ), null);
    }

    @Override
    public void envoyerVerificationEmail(String destinataire, String prenom, String token) {
        publier(TypeEmail.VERIFICATION_EMAIL, EmailSortant.html(
                destinataire,
                SUJET_MAIL_VERIFICATION_EMAIL,
                String.format(CONTENU_MAIL_VERIFICATION_EMAIL, echapper(prenom), appUrl + "auth/verifier-email?token=" + token)
        ), null);
    }

    @Override
    public void envoyerTentativeInscription(String destinataire) {
        publier(TypeEmail.TENTATIVE_INSCRIPTION, EmailSortant.html(
                destinataire,
                SUJET_MAIL_TENTATIVE_INSCRIPTION,
                String.format(CONTENU_MAIL_TENTATIVE_INSCRIPTION, appUrl + "auth/forgot")
        ), null);
    }

    @Override
    public void sendPasswordResetMail(String destinataire, String resetToken) {
        publier(TypeEmail.RESET_MOT_DE_PASSE, EmailSortant.html(
                destinataire,
                SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE,
                String.format(CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE, resetLink + resetToken)
        ), null);
    }

    @Override
    public void sendPasswordUpdatedConfirmation(String destinataire) {
        publier(TypeEmail.MOT_DE_PASSE_MODIFIE, EmailSortant.html(
                destinataire,
                SUJET_MAIL_CONFIRMATION_MOT_DE_PASSE,
                String.format(CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE, urlLogin)
        ), null);
    }

    @Override
    public void envoyerInvitationSignature(Contrat contrat, String token) {
        String prenomLocataire = fullName(contrat.getLocataire().getFirstName(),
                contrat.getLocataire().getLastName());
        String prenomProprietaire = fullName(contrat.getProprietaire().getFirstName(),
                contrat.getProprietaire().getLastName());
        String lienSignature = appUrl + "contrats/signer/" + token;

        publier(TypeEmail.INVITATION_SIGNATURE_CONTRAT, EmailSortant.html(
                contrat.getLocataire().getMail(),
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
                        lienSignature)
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

        publier(TypeEmail.INVITATION_SIGNATURE_EDL, EmailSortant.html(
                edl.getLocataire().getMail(),
                SUJET_MAIL_INVITATION_SIGNATURE_EDL,
                String.format(CONTENU_MAIL_INVITATION_SIGNATURE_EDL,
                        prenomNomLocataire,
                        prenomNomProprietaire,
                        adresse,
                        typeEdl,
                        edl.getDateRealisation().toString(),
                        lienSignature)
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
    public void envoyerDemandeLocation(DemandeLocation demande) {
        publier(TypeEmail.DEMANDE_LOCATION, EmailSortant.html(
                demande.getLocataire().getMail(),
                SUJET_MAIL_DEMANDE_LOCATION,
                String.format(CONTENU_MAIL_DEMANDE_LOCATION,
                        fullName(demande.getLocataire().getFirstName(), demande.getLocataire().getLastName()),
                        fullName(demande.getProprietaire().getFirstName(), demande.getProprietaire().getLastName()),
                        buildAdresse(demande.getBien()),
                        echapper(demande.getBien().getTitre()),
                        demande.getDateExpiration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm")),
                        appUrl + "demandes-location")
        ), null);
        log.debug("E-mail de demande de location {} préparé", demande.getId());
    }

    @Override
    public void envoyerConge(Location location, User destinataire, boolean retire) {
        Bien bien = location.getBien();
        boolean auProprietaire = destinataire.getId().equals(location.getProprietaire().getId());
        User auteur = auProprietaire ? location.getLocataire() : location.getProprietaire();
        String adresse = buildAdresse(bien);
        String date = location.getDateFinPrevue() != null
                ? location.getDateFinPrevue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
        String message = retire
                ? "<strong>" + fullName(auteur.getFirstName(), auteur.getLastName()) + "</strong> a retiré son congé "
                  + "pour le logement situé au <strong>" + adresse + "</strong> : le bail continue."
                : "<strong>" + fullName(auteur.getFirstName(), auteur.getLastName()) + "</strong> a donné congé pour "
                  + "le logement situé au <strong>" + adresse + "</strong>. Le bail prendra fin le <strong>" + date
                  + "</strong>. Pensez à prévoir l'état des lieux de sortie.";
        String lien = auProprietaire ? appUrl + "biens/" + bien.getId() : appUrl + "mon-logement?bien=" + bien.getId();
        publier(TypeEmail.CONGE, EmailSortant.html(
                destinataire.getMail(),
                retire ? "Kupanga : congé retiré" : "Kupanga : congé pour votre location",
                String.format(CONTENU_MAIL_FIN_RESERVATION,
                        fullName(destinataire.getFirstName(), destinataire.getLastName()),
                        retire ? "Congé retiré" : "Congé", message, "Voir le logement", lien, adresse,
                        echapper(bien.getTitre()))
        ), null);
        log.debug("E-mail de congé (location {}) préparé", location.getId());
    }

    @Override
    public void envoyerFinDeBail(Location location) {
        Bien bien = location.getBien();
        User locataire = location.getLocataire();
        String adresse = buildAdresse(bien);
        FormatMontant montants = juridictionRegistry.formatMontant(bien.getPays(), bien.getDevise());
        String depot = location.getDepotRestitue() != null ? montants.f(location.getDepotRestitue()) : "—";
        String message = "Le bail du logement situé au <strong>" + adresse + "</strong> est terminé. "
                + "Dépôt de garantie restitué : <strong>" + depot + "</strong>."
                + (location.getMotifRetenue() != null
                   ? " Motif de la retenue : " + echapper(location.getMotifRetenue()) + "." : "")
                + " Votre bail, vos états des lieux et vos quittances restent disponibles dans Kupanga.";
        publier(TypeEmail.FIN_DE_BAIL, EmailSortant.html(
                locataire.getMail(),
                "Kupanga : fin de votre bail",
                String.format(CONTENU_MAIL_FIN_RESERVATION,
                        fullName(locataire.getFirstName(), locataire.getLastName()),
                        "Fin du bail", message, "Mes documents", appUrl + "documents", adresse,
                        echapper(bien.getTitre()))
        ), null);
        log.debug("E-mail de fin de bail (location {}) préparé", location.getId());
    }

    @Override
    public void envoyerFinReservation(Location location, StatutLocation motif, User destinataire) {
        Bien bien = location.getBien();
        boolean auProprietaire = destinataire.getId().equals(location.getProprietaire().getId());
        String adresse = buildAdresse(bien);
        String candidat = fullName(location.getLocataire().getFirstName(), location.getLocataire().getLastName());
        String proprietaire = fullName(location.getProprietaire().getFirstName(),
                location.getProprietaire().getLastName());
        String sujet;
        String surtitre;
        String message;
        switch (motif) {
            case DESISTEE -> {
                sujet = "Kupanga : désistement pour votre logement";
                surtitre = "Désistement";
                message = "<strong>" + candidat + "</strong> s'est désisté pour votre logement situé au <strong>"
                        + adresse + "</strong>, avant de signer le bail. Le bail non signé est annulé et l'annonce "
                        + "est de nouveau en ligne : vous pouvez proposer le logement à une autre personne.";
            }
            case ANNULEE -> {
                sujet = "Kupanga : votre réservation a été retirée";
                surtitre = "Réservation retirée";
                message = "<strong>" + proprietaire + "</strong> a retiré votre réservation du logement situé au "
                        + "<strong>" + adresse + "</strong>. Le bail non signé est annulé.";
            }
            case EXPIREE -> {
                sujet = "Kupanga : la réservation a expiré";
                surtitre = "Réservation expirée";
                message = auProprietaire
                        ? "Le bail de votre logement situé au <strong>" + adresse + "</strong> n'a pas été signé "
                          + "dans les 14 jours : la réservation de <strong>" + candidat + "</strong> a pris fin "
                          + "et l'annonce est de nouveau en ligne."
                        : "Votre réservation du logement situé au <strong>" + adresse + "</strong> a pris fin : "
                          + "le bail n'a pas été signé dans les 14 jours.";
            }
            default -> throw new IllegalArgumentException("Motif de fin de réservation inattendu : " + motif);
        }
        String bouton = auProprietaire ? "Proposer la location" : "Voir les annonces";
        String lien = auProprietaire ? appUrl + "biens/" + bien.getId() + "/locataire" : appUrl + "explorer";

        publier(TypeEmail.FIN_RESERVATION, EmailSortant.html(
                destinataire.getMail(),
                sujet,
                String.format(CONTENU_MAIL_FIN_RESERVATION,
                        fullName(destinataire.getFirstName(), destinataire.getLastName()),
                        surtitre, message, bouton, lien, adresse, echapper(bien.getTitre()))
        ), null);
        log.debug("E-mail de fin de réservation {} ({}) préparé", location.getId(), motif);
    }

    @Override
    public void envoyerQuittance(Quittance quittance) {
        String moisLabel = echapper(quittance.getMois() + " " + quittance.getAnnee());
        String adresse = buildAdresse(quittance.getBien());

        PdfAJoindre pdf = pdfAJoindre(QUITTANCE_BUCKET, quittance.getClePdf(),
                String.format("Quittance_%d_%d_%s.pdf", quittance.getId(), quittance.getAnnee(), quittance.getMois()));

        publier(TypeEmail.QUITTANCE, EmailSortant.html(
                quittance.getLocataire().getMail(),
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
                                ? quittance.getDatePaiement().toString() : "—")
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

        publier(TypeEmail.CONTRAT_SIGNE, EmailSortant.html(
                destinataire,
                SUJET_MAIL_CONTRAT_SIGNE,
                String.format(CONTENU_MAIL_CONTRAT_SIGNE,
                        prenomNom,
                        echapper(contrat.getAdresseBien()),
                        montants(contrat).f(contrat.getLoyerMensuel()),
                        montants(contrat).f(contrat.getChargesMensuelles()),
                        montants(contrat).f(contrat.getDepotGarantie()),
                        contrat.getDateDebut(),
                        contrat.getDureeBailMois())
        ), pdf);
    }

    private void envoyerConfirmationEdl(EtatDesLieux edl, String destinataire, String prenomNom) {
        String adresse = buildAdresse(edl.getBien());
        String typeEdl = resolveTypeEdl(edl);

        PdfAJoindre pdf = pdfAJoindre(EDL_BUCKET, edl.getClePdf(),
                String.format("EDL_%s_%d.pdf", edl.getType().name(), edl.getId()));

        publier(TypeEmail.EDL_SIGNE, EmailSortant.html(
                destinataire,
                SUJET_MAIL_EDL_SIGNE,
                String.format(CONTENU_MAIL_EDL_SIGNE,
                        prenomNom,
                        adresse,
                        typeEdl,
                        edl.getDateRealisation().toString())
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

    /**
     * M2 : met l'e-mail en file, dans la transaction en cours (B11 : rien n'est envoyé si elle est annulée) ;
     * il part après le commit, avec de nouveaux essais en cas de panne.
     */
    private void publier(TypeEmail type, EmailSortant email, PdfAJoindre pdf) {
        fileEmails.ajouter(email, type, pdf == null ? List.of() : List.of(pdf));
    }
}
