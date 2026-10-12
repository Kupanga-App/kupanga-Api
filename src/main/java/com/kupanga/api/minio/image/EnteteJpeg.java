package com.kupanga.api.minio.image;

/**
 * R1 (revue) : reconnaît un JPEG « baseline » (marqueur de trame SOF0 ou SOF1) en parcourant les segments
 * jusqu'au premier marqueur de trame, sans décoder. Prudent : toute anomalie (octet parasite entre segments,
 * longueur invalide, fin de fichier, autre type de trame) donne « non baseline ». La libjpeg du JDK tolère
 * ces anomalies et décoderait quand même un JPEG progressif : un analyseur permissif se contournerait.
 */
final class EnteteJpeg {

    private EnteteJpeg() {
    }

    static boolean estJpeg(byte[] contenu) {
        return contenu != null && contenu.length >= 2
                && (contenu[0] & 0xFF) == 0xFF && (contenu[1] & 0xFF) == 0xD8;
    }

    /** @return {@code true} seulement si une trame SOF0/SOF1 est trouvée sans aucune anomalie avant elle */
    static boolean estBaseline(byte[] jpeg) {
        if (!estJpeg(jpeg)) return false;
        int i = 2;
        while (i + 4 <= jpeg.length) {
            if ((jpeg[i] & 0xFF) != 0xFF) return false;
            int marqueur = jpeg[i + 1] & 0xFF;
            if (marqueur == 0xFF) { // octet de remplissage autorisé par la norme
                i++;
                continue;
            }
            if (marqueur == 0xC0 || marqueur == 0xC1) return true;
            // Autres trames (progressive, sans perte, arithmétique…), début des données ou fin d'image
            if ((marqueur >= 0xC2 && marqueur <= 0xCF && marqueur != 0xC4 && marqueur != 0xC8 && marqueur != 0xCC)
                    || marqueur == 0xDA || marqueur == 0xD9) {
                return false;
            }
            int longueur = ((jpeg[i + 2] & 0xFF) << 8) | (jpeg[i + 3] & 0xFF);
            if (longueur < 2) return false;
            i += 2 + longueur;
        }
        return false;
    }
}
