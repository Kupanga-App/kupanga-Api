package com.kupanga.api.minio.image;

import com.kupanga.api.exception.business.KupangaBusinessException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * R1 (revue) : retire les blocs {@code EXIF} et {@code XMP } d'un WEBP (position GPS) sans décoder l'image,
 * et efface les indicateurs correspondants du bloc {@code VP8X}. Le WEBP n'est pas décodable en Java pur :
 * il est stocké nettoyé, sans réduction.
 */
final class NettoyageWebp {

    private static final int DRAPEAU_EXIF = 0x08;
    private static final int DRAPEAU_XMP = 0x04;

    private NettoyageWebp() {
    }

    /**
     * @throws KupangaBusinessException 400 si la structure RIFF est invalide
     */
    static byte[] nettoyer(byte[] webp) {
        if (webp.length < 12 || !texte(webp, 0).equals("RIFF") || !texte(webp, 8).equals("WEBP")) {
            throw illisible();
        }
        long tailleRiff = u32(webp, 4);
        long fin = Math.min(webp.length, 8 + tailleRiff);
        ByteArrayOutputStream blocs = new ByteArrayOutputStream(webp.length);
        int i = 12;
        while (i + 8 <= fin) {
            String type = texte(webp, i);
            long taille = u32(webp, i + 4);
            long tailleTotale = 8 + taille + (taille & 1);
            if (i + 8 + taille > fin) throw illisible();
            if (!type.equals("EXIF") && !type.equals("XMP ")) {
                int longueur = (int) Math.min(tailleTotale, fin - i);
                byte[] bloc = new byte[longueur];
                System.arraycopy(webp, i, bloc, 0, longueur);
                if (type.equals("VP8X") && longueur > 8) {
                    bloc[8] = (byte) (bloc[8] & ~(DRAPEAU_EXIF | DRAPEAU_XMP));
                }
                blocs.writeBytes(bloc);
            }
            i += (int) tailleTotale;
        }
        byte[] corps = blocs.toByteArray();
        ByteArrayOutputStream resultat = new ByteArrayOutputStream(corps.length + 12);
        resultat.writeBytes("RIFF".getBytes(StandardCharsets.ISO_8859_1));
        long nouvelleTaille = 4L + corps.length;
        for (int k = 0; k < 4; k++) resultat.write((int) (nouvelleTaille >> (8 * k)) & 0xFF);
        resultat.writeBytes("WEBP".getBytes(StandardCharsets.ISO_8859_1));
        resultat.writeBytes(corps);
        return resultat.toByteArray();
    }

    private static String texte(byte[] b, int i) {
        return new String(b, i, 4, StandardCharsets.ISO_8859_1);
    }

    private static long u32(byte[] b, int i) {
        return (b[i] & 0xFFL) | (b[i + 1] & 0xFFL) << 8 | (b[i + 2] & 0xFFL) << 16 | (b[i + 3] & 0xFFL) << 24;
    }

    private static KupangaBusinessException illisible() {
        return new KupangaBusinessException("Photo illisible ou endommagée", HttpStatus.BAD_REQUEST);
    }
}
