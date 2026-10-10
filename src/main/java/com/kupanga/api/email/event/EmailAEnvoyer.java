package com.kupanga.api.email.event;

import com.kupanga.api.email.dto.BrevoEmail;

import java.util.List;

/**
 * B11 : e-mail entièrement préparé dans la transaction de l'appelant (aucune entité JPA à charger plus tard),
 * envoyé par {@link EmailEnvoiListener} seulement après le commit.
 *
 * @param email e-mail Brevo sans pièce jointe
 * @param pdfs  PDF à joindre, lus dans MinIO au moment de l'envoi
 */
public record EmailAEnvoyer(BrevoEmail email, List<PdfAJoindre> pdfs) {

    public record PdfAJoindre(String bucket, String cle, String nomFichier) {}
}
