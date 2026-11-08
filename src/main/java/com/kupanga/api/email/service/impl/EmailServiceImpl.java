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

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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

    /** Dates des e-mails : « 1 décembre 2026 » (au lieu du toString() ISO « 2026-12-01 »). */
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    /** Date et heure : « 11 novembre 2026 à 18 h 30 ». */
    private static final DateTimeFormatter FORMAT_DATE_HEURE =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'à' H 'h' mm", Locale.FRENCH);

    /** Tons des badges d'état, identiques aux badges de l'interface (fond, texte, point). */
    private enum Ton {
        SUCCES("#e5f1e8", "#22663a", "#2f8a4e"),
        ATTENTE("#fbefdd", "#8a5a12", "#b7791f"),
        ALERTE("#fbe9e7", "#8e1b12", "#b42318"),
        NEUTRE("#edefe8", "#46545c", "#66737a");

        final String fond;
        final String texte;
        final String point;

        Ton(String fond, String texte, String point) {
            this.fond = fond;
            this.texte = texte;
            this.point = point;
        }
    }

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
                finaliser(String.format(CONTENU_MAIL_BIENVENUE_PROFIL_COMPLETE, echapper(prenom), echapper(prenom)))
        ), null);
    }

    @Override
    public void envoyerVerificationEmail(String destinataire, String prenom, String token) {
        publier(TypeEmail.VERIFICATION_EMAIL, EmailSortant.html(
                destinataire,
                SUJET_MAIL_VERIFICATION_EMAIL,
                finaliser(String.format(CONTENU_MAIL_VERIFICATION_EMAIL, echapper(prenom),
                        appUrl + "auth/verifier-email?token=" + token))
        ), null);
    }

    @Override
    public void envoyerTentativeInscription(String destinataire) {
        publier(TypeEmail.TENTATIVE_INSCRIPTION, EmailSortant.html(
                destinataire,
                SUJET_MAIL_TENTATIVE_INSCRIPTION,
                finaliser(String.format(CONTENU_MAIL_TENTATIVE_INSCRIPTION, appUrl + "auth/forgot"))
        ), null);
    }

    @Override
    public void sendPasswordResetMail(String destinataire, String resetToken) {
        publier(TypeEmail.RESET_MOT_DE_PASSE, EmailSortant.html(
                destinataire,
                SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE,
                finaliser(String.format(CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE, resetLink + resetToken))
        ), null);
    }

    @Override
    public void sendPasswordUpdatedConfirmation(String destinataire) {
        publier(TypeEmail.MOT_DE_PASSE_MODIFIE, EmailSortant.html(
                destinataire,
                SUJET_MAIL_CONFIRMATION_MOT_DE_PASSE,
                finaliser(String.format(CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE, urlLogin))
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
                finaliser(String.format(CONTENU_MAIL_INVITATION_SIGNATURE,
                        prenomProprietaire,
                        prenomLocataire,
                        prenomProprietaire,
                        echapper(contrat.getAdresseBien()),
                        montants(contrat).f(contrat.getLoyerMensuel()),
                        montants(contrat).f(contrat.getChargesMensuelles()),
                        montants(contrat).f(contrat.getDepotGarantie()),
                        date(contrat.getDateDebut()),
                        contrat.getDureeBailMois(),
                        lienSignature))
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
                finaliser(String.format(CONTENU_MAIL_INVITATION_SIGNATURE_EDL,
                        prenomNomLocataire,
                        prenomNomProprietaire,
                        adresse,
                        typeEdl,
                        date(edl.getDateRealisation()),
                        lienSignature))
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
                finaliser(String.format(CONTENU_MAIL_DEMANDE_LOCATION,
                        fullName(demande.getLocataire().getFirstName(), demande.getLocataire().getLastName()),
                        fullName(demande.getProprietaire().getFirstName(), demande.getProprietaire().getLastName()),
                        buildAdresse(demande.getBien()),
                        echapper(demande.getBien().getTitre()),
                        dateHeure(demande.getDateExpiration()),
                        appUrl + "demandes-location"))
        ), null);
        log.debug("E-mail de demande de location {} préparé", demande.getId());
    }

    @Override
    public void envoyerConge(Location location, User destinataire, boolean retire) {
        Bien bien = location.getBien();
        boolean auProprietaire = destinataire.getId().equals(location.getProprietaire().getId());
        User auteur = auProprietaire ? location.getLocataire() : location.getProprietaire();
        String adresse = buildAdresse(bien);
        String nomAuteur = fullName(auteur.getFirstName(), auteur.getLastName());
        String date = date(location.getDateFinPrevue());
        String lignesLogement = ligne("Logement", echapper(bien.getTitre())) + ligne("Adresse", adresse);
        String titre;
        String message;
        String recap;
        String apercu;
        String badge;
        if (retire) {
            titre = "Le congé a été retiré";
            message = "<strong>" + nomAuteur + "</strong> a retiré son congé : le bail continue normalement.";
            recap = lignesLogement + ligne("Congé retiré par", nomAuteur);
            apercu = nomAuteur + " a retiré son congé : le bail continue.";
            badge = badge("Le bail continue", Ton.SUCCES);
        } else {
            titre = "Un congé a été donné";
            message = "<strong>" + nomAuteur + "</strong> a donné congé. Le bail prendra fin le <strong>" + date
                    + "</strong>. Pensez à prévoir l'état des lieux de sortie.";
            recap = lignesLogement + ligne("Congé donné par", nomAuteur) + ligne("Fin du bail", date);
            apercu = nomAuteur + " a donné congé. Fin du bail le " + date + ".";
            badge = badge("Congé donné", Ton.ATTENTE);
        }
        String lien = auProprietaire ? appUrl + "biens/" + bien.getId() : appUrl + "mon-logement?bien=" + bien.getId();
        publier(TypeEmail.CONGE, EmailSortant.html(
                destinataire.getMail(),
                retire ? "Congé retiré · Kupanga" : "Congé donné pour votre location · Kupanga",
                finaliser(String.format(CONTENU_MAIL_FIN_RESERVATION,
                        fullName(destinataire.getFirstName(), destinataire.getLastName()),
                        badge, titre, message, "Voir le logement", lien, recap, apercu))
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
        String message = "Le bail de ce logement est terminé. "
                + "Votre bail, vos états des lieux et vos quittances restent disponibles dans Kupanga.";
        String recap = ligne("Logement", echapper(bien.getTitre())) + ligne("Adresse", adresse)
                + ligne("Dépôt de garantie restitué", depot)
                // Montant de la retenue : voir changements-textes.md (variable à ajouter si le dépôt initial est connu).
                + (location.getMotifRetenue() != null
                   ? ligne("Motif de la retenue", echapper(location.getMotifRetenue())) : "");
        publier(TypeEmail.FIN_DE_BAIL, EmailSortant.html(
                locataire.getMail(),
                "Votre bail est terminé · Kupanga",
                finaliser(String.format(CONTENU_MAIL_FIN_RESERVATION,
                        fullName(locataire.getFirstName(), locataire.getLastName()),
                        badge("Bail terminé", Ton.NEUTRE), "Votre bail est terminé", message,
                        "Mes documents", appUrl + "documents", recap,
                        "Votre bail est terminé. Dépôt de garantie restitué : " + depot + "."))
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
        // L'autre partie : le candidat pour le propriétaire, le propriétaire pour le candidat.
        String lignesLogement = ligne("Logement", echapper(bien.getTitre())) + ligne("Adresse", adresse)
                + (auProprietaire ? ligne("Candidat", candidat) : ligne("Propriétaire", proprietaire));
        String sujet;
        String badge;
        String titre;
        String message;
        String recap = lignesLogement;
        String apercu;
        switch (motif) {
            case DESISTEE -> {
                sujet = "Désistement pour votre logement · Kupanga";
                badge = badge("Désistement", Ton.ATTENTE);
                titre = "Désistement pour votre logement";
                // Formulation sans accord : l'API ne connaît pas le genre de la personne.
                message = "<strong>" + candidat + "</strong> a renoncé à la location avant de signer le bail. "
                        + "Le bail non signé est annulé et votre annonce est de nouveau en ligne : "
                        + "vous pouvez proposer le logement à une autre personne.";
                apercu = candidat + " a renoncé à la location. Votre annonce est de nouveau en ligne.";
            }
            case ANNULEE -> {
                sujet = "Votre réservation a été retirée · Kupanga";
                badge = badge("Réservation retirée", Ton.NEUTRE);
                titre = "Votre réservation a été retirée";
                message = "<strong>" + proprietaire + "</strong> a retiré votre réservation. Le bail, qui n'était "
                        + "pas encore signé, est annulé. Vous pouvez chercher un autre logement dans les annonces.";
                apercu = proprietaire + " a retiré votre réservation. Le bail non signé est annulé.";
            }
            case EXPIREE -> {
                sujet = auProprietaire ? "La réservation a expiré · Kupanga" : "Votre réservation a expiré · Kupanga";
                badge = badge("Réservation expirée", Ton.NEUTRE);
                titre = auProprietaire ? "La réservation a expiré" : "Votre réservation a expiré";
                message = auProprietaire
                        ? "Le bail n'a pas été signé dans les 14 jours : la réservation de <strong>" + candidat
                          + "</strong> a pris fin et votre annonce est de nouveau en ligne."
                        : "Le bail n'a pas été signé dans les 14 jours : votre réservation a pris fin. "
                          + "Vous pouvez chercher un autre logement dans les annonces.";
                recap = lignesLogement + ligne("Délai", "14 jours sans signature du bail");
                apercu = auProprietaire
                        ? "Le bail n'a pas été signé dans les 14 jours. Votre annonce est de nouveau en ligne."
                        : "Le bail n'a pas été signé dans les 14 jours : votre réservation a pris fin.";
            }
            default -> throw new IllegalArgumentException("Motif de fin de réservation inattendu : " + motif);
        }
        String bouton = auProprietaire ? "Proposer la location" : "Voir les annonces";
        String lien = auProprietaire ? appUrl + "biens/" + bien.getId() + "/locataire" : appUrl + "explorer";

        publier(TypeEmail.FIN_RESERVATION, EmailSortant.html(
                destinataire.getMail(),
                sujet,
                finaliser(String.format(CONTENU_MAIL_FIN_RESERVATION,
                        fullName(destinataire.getFirstName(), destinataire.getLastName()),
                        badge, titre, message, bouton, lien, recap, apercu))
        ), null);
        log.debug("E-mail de fin de réservation {} ({}) préparé", location.getId(), motif);
    }

    @Override
    public void envoyerQuittance(Quittance quittance) {
        String moisLabel = echapper(quittance.getMois() + " " + quittance.getAnnee());
        String adresse = buildAdresse(quittance.getBien());

        String nomPdf = String.format("Quittance_%d_%d_%s.pdf",
                quittance.getId(), quittance.getAnnee(), quittance.getMois());
        PdfAJoindre pdf = pdfAJoindre(QUITTANCE_BUCKET, quittance.getClePdf(), nomPdf);

        publier(TypeEmail.QUITTANCE, EmailSortant.html(
                quittance.getLocataire().getMail(),
                SUJET_MAIL_QUITTANCE,
                finaliser(String.format(CONTENU_MAIL_QUITTANCE,
                        fullName(quittance.getLocataire().getFirstName(),
                                quittance.getLocataire().getLastName()),
                        moisLabel,
                        adresse,
                        montants(quittance).f(quittance.getLoyerMensuel()),
                        montants(quittance).f(quittance.getChargesMensuelles()),
                        montants(quittance).f(quittance.getMontantTotal()),
                        date(quittance.getDatePaiement()),
                        blocPieceJointe(pdf, nomPdf, "Quittance de loyer")))
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
        String nomPdf = "Contrat_" + contrat.getId() + ".pdf";
        PdfAJoindre pdf = pdfAJoindre(CONTRAT_BUCKET, contrat.getClePdf(), nomPdf);

        publier(TypeEmail.CONTRAT_SIGNE, EmailSortant.html(
                destinataire,
                SUJET_MAIL_CONTRAT_SIGNE,
                finaliser(String.format(CONTENU_MAIL_CONTRAT_SIGNE,
                        prenomNom,
                        echapper(contrat.getAdresseBien()),
                        montants(contrat).f(contrat.getLoyerMensuel()),
                        montants(contrat).f(contrat.getChargesMensuelles()),
                        montants(contrat).f(contrat.getDepotGarantie()),
                        date(contrat.getDateDebut()),
                        contrat.getDureeBailMois(),
                        blocPieceJointe(pdf, nomPdf, "Bail signé")))
        ), pdf);
    }

    private void envoyerConfirmationEdl(EtatDesLieux edl, String destinataire, String prenomNom) {
        String adresse = buildAdresse(edl.getBien());
        String typeEdl = resolveTypeEdl(edl);

        String nomPdf = String.format("EDL_%s_%d.pdf", edl.getType().name(), edl.getId());
        PdfAJoindre pdf = pdfAJoindre(EDL_BUCKET, edl.getClePdf(), nomPdf);

        publier(TypeEmail.EDL_SIGNE, EmailSortant.html(
                destinataire,
                SUJET_MAIL_EDL_SIGNE,
                finaliser(String.format(CONTENU_MAIL_EDL_SIGNE,
                        prenomNom,
                        adresse,
                        typeEdl,
                        date(edl.getDateRealisation()),
                        blocPieceJointe(pdf, nomPdf, typeEdl + " signé")))
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

    /** « 1 décembre 2026 », ou « — » si la date est absente. */
    private String date(LocalDate date) {
        return date == null ? "—" : date.format(FORMAT_DATE);
    }

    /** « 11 novembre 2026 à 18 h 30 », ou « — » si la date est absente. */
    private String dateHeure(LocalDateTime dateHeure) {
        return dateHeure == null ? "—" : dateHeure.format(FORMAT_DATE_HEURE);
    }

    /** Une ligne du récapitulatif (libellé au-dessus de la valeur) ; la valeur doit déjà être échappée. */
    private String ligne(String libelle, String valeur) {
        return String.format(LIGNE_RECAP, libelle, valeur == null ? "—" : valeur);
    }

    private String badge(String libelle, Ton ton) {
        return String.format(BLOC_BADGE, ton.fond, ton.texte, ton.point, libelle);
    }

    /** Bloc « Document joint », seulement si un PDF part réellement avec l'e-mail. */
    private String blocPieceJointe(PdfAJoindre pdf, String nomFichier, String nature) {
        return pdf == null ? "" : String.format(BLOC_PIECE_JOINTE, echapper(nomFichier), nature);
    }

    /** Remplace les jetons communs à tous les modèles (lien et domaine du pied de page, paramétrables via app.url). */
    private String finaliser(String html) {
        return html.replace("{{URL_APP}}", appUrl).replace("{{DOMAINE_APP}}", domaine());
    }

    private String domaine() {
        String hote = URI.create(appUrl).getHost();
        return hote != null ? hote : appUrl;
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
