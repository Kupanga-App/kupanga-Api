package com.kupanga.api.stockage.image;

/** R1 : contenu prêt à envoyer, avec le format fixé par le serveur (nom d'objet et type MIME). */
public record FichierImage(byte[] contenu, FormatImage format) {
}
