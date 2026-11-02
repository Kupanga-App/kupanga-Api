package com.kupanga.api.email.file;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * M2 : PDF à joindre, désigné par sa clé dans un bucket privé (P0-7) et lu au moment de l'envoi : la file ne garde
 * jamais le contenu des documents.
 */
@Embeddable
public record PdfAJoindre(
        @Column(name = "bucket", nullable = false, length = 100) String bucket,
        @Column(name = "cle", nullable = false, length = 255) String cle,
        @Column(name = "nom_fichier", nullable = false, length = 255) String nomFichier
) {
}
