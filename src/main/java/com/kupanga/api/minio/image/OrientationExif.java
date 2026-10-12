package com.kupanga.api.minio.image;

/**
 * R1 : lit l'orientation EXIF (balise 0x0112) d'un JPEG. Les smartphones enregistrent la photo « couchée » et
 * indiquent la rotation dans l'EXIF ; ImageIO l'ignore et le réencodage retire l'EXIF : sans elle, la photo
 * apparaîtrait tournée. Lecture bornée, sans dépendance ; toute donnée inattendue donne 1 (aucune rotation).
 */
final class OrientationExif {

    static final int AUCUNE = 1;

    private static final int BALISE_ORIENTATION = 0x0112;

    private OrientationExif() {
    }

    /** @return l'orientation EXIF (1 à 8), ou 1 si absente, invalide ou si le fichier n'est pas un JPEG */
    static int lire(byte[] jpeg) {
        try {
            if (jpeg == null || jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
                return AUCUNE;
            }
            int i = 2;
            while (i + 4 <= jpeg.length && (jpeg[i] & 0xFF) == 0xFF) {
                int marqueur = jpeg[i + 1] & 0xFF;
                if (marqueur == 0xDA || marqueur == 0xD9) break; // début des données d'image : plus d'EXIF
                int longueur = u16(jpeg, i + 2, true);
                if (longueur < 2 || i + 2 + longueur > jpeg.length) break;
                if (marqueur == 0xE1 && estExif(jpeg, i + 4, longueur - 2)) {
                    return orientationTiff(jpeg, i + 10, i + 2 + longueur);
                }
                i += 2 + longueur;
            }
        } catch (RuntimeException e) {
            // Fichier tronqué ou malformé : on n'applique aucune rotation
        }
        return AUCUNE;
    }

    private static boolean estExif(byte[] b, int debut, int longueur) {
        return longueur >= 6 && b[debut] == 'E' && b[debut + 1] == 'x' && b[debut + 2] == 'i' && b[debut + 3] == 'f'
                && b[debut + 4] == 0 && b[debut + 5] == 0;
    }

    /** En-tête TIFF à {@code tiff}, segment terminé à {@code fin} (exclu). */
    private static int orientationTiff(byte[] b, int tiff, int fin) {
        if (tiff + 8 > fin) return AUCUNE;
        boolean grandBoutiste;
        if (b[tiff] == 'M' && b[tiff + 1] == 'M') grandBoutiste = true;
        else if (b[tiff] == 'I' && b[tiff + 1] == 'I') grandBoutiste = false;
        else return AUCUNE;
        long decalage = u32(b, tiff + 4, grandBoutiste);
        if (decalage < 8 || tiff + decalage + 2 > fin) return AUCUNE;
        int ifd = (int) (tiff + decalage);
        int entrees = u16(b, ifd, grandBoutiste);
        for (int n = 0; n < entrees; n++) {
            int entree = ifd + 2 + n * 12;
            if (entree + 12 > fin) return AUCUNE;
            if (u16(b, entree, grandBoutiste) == BALISE_ORIENTATION) {
                int valeur = u16(b, entree + 8, grandBoutiste);
                return valeur >= 1 && valeur <= 8 ? valeur : AUCUNE;
            }
        }
        return AUCUNE;
    }

    private static int u16(byte[] b, int i, boolean grandBoutiste) {
        int a = b[i] & 0xFF;
        int c = b[i + 1] & 0xFF;
        return grandBoutiste ? (a << 8) | c : (c << 8) | a;
    }

    private static long u32(byte[] b, int i, boolean grandBoutiste) {
        long resultat = 0;
        for (int k = 0; k < 4; k++) {
            int octet = b[grandBoutiste ? i + k : i + 3 - k] & 0xFF;
            resultat = (resultat << 8) | octet;
        }
        return resultat;
    }
}
