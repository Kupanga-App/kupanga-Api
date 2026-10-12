package com.kupanga.api.minio.image;

/** R1 : contenu prêt à envoyer, avec le format fixé par le serveur (nom d'objet et type MIME). */
public record FichierImage(byte[] contenu, FormatImage format) {
}
