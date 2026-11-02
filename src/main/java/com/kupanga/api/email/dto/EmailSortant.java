package com.kupanga.api.email.dto;

import java.util.List;

/**
 * M1 : e-mail transactionnel, indépendant du fournisseur d'envoi (SMTP générique). L'expéditeur n'y figure pas :
 * il vient de la configuration ({@code email.*}).
 *
 * @param destinataire  adresse du destinataire
 * @param sujet         sujet (peut contenir un prénom : jamais journalisé)
 * @param html          corps HTML (valeurs saisies déjà échappées)
 * @param texte         version texte brut du même message
 * @param piecesJointes pièces jointes (PDF), déjà lues
 */
public record EmailSortant(
        String destinataire,
        String sujet,
        String html,
        String texte,
        List<PieceJointe> piecesJointes
) {

    public EmailSortant {
        piecesJointes = piecesJointes == null ? List.of() : List.copyOf(piecesJointes);
    }

    /** E-mail HTML sans pièce jointe ; la version texte est déduite du HTML. */
    public static EmailSortant html(String destinataire, String sujet, String html) {
        return new EmailSortant(destinataire, sujet, html, TexteBrut.depuisHtml(html), List.of());
    }

    public EmailSortant avecPiecesJointes(List<PieceJointe> pieces) {
        return new EmailSortant(destinataire, sujet, html, texte, pieces);
    }

    /** @param typeMime ex. {@code application/pdf} */
    public record PieceJointe(String nomFichier, byte[] contenu, String typeMime) {
    }
}
