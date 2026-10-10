package com.kupanga.api.immobilier.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Valide la structure complète du PNG (et pas seulement l'en-tête) : le moteur PDF (iText de Flying Saucer)
 * lit tous les blocs, prend les dimensions du dernier IHDR et décompresse en mémoire les blocs compressés
 * comme {@code iCCP}. Un seul IHDR, en premier ; IEND en dernier ; blocs limités à une liste blanche ;
 * profondeur 8 bits. Seul bloc compressé admis (hors IDAT) : le profil de couleur {@code iCCP}, que Safari
 * peut ajouter, limité à {@link #ICCP_MAX_OCTETS} (deflate ≤ ~1 000:1, soit ~4 Mo décompressés au pire).
 */
public class SignaturePngValidator implements ConstraintValidator<SignaturePng, String> {

    /** Le pad du front fait 480 × 140 px : une signature réelle pèse quelques dizaines de Ko en base64. */
    public static final int TAILLE_MAX_BASE64 = 200_000;
    static final int LARGEUR_MAX = 2000;
    static final int HAUTEUR_MAX = 1000;

    private static final byte[] SIGNATURE_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    /** Blocs produits par les canvas des navigateurs et les encodeurs usuels ; seul iCCP est compressé (hors IDAT). */
    private static final Set<String> BLOCS_AUTORISES = Set.of(
            "IHDR", "PLTE", "tRNS", "IDAT", "IEND", "sRGB", "gAMA", "pHYs", "cHRM", "sBIT", "bKGD", "tIME",
            "iCCP", "iDOT"); // iDOT : bloc Apple non compressé

    static final int ICCP_MAX_OCTETS = 4096;

    /** Types de couleur : 2 RVB, 3 palette, 6 RVBA (canvas) ; 0 et 4 (niveaux de gris) aussi admis. */
    private static final Set<Integer> TYPES_COULEUR = Set.of(0, 2, 3, 4, 6);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return true; // @NotBlank s'en charge
        if (value.length() > TAILLE_MAX_BASE64) return false;

        byte[] image;
        try {
            image = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return pngValide(image);
    }

    static boolean pngValide(byte[] image) {
        if (image.length < 8 || !Arrays.equals(image, 0, 8, SIGNATURE_PNG, 0, 8)) return false;

        ByteBuffer buffer = ByteBuffer.wrap(image);
        int position = 8;
        boolean premier = true;
        boolean idat = false;

        while (position < image.length) {
            if (image.length - position < 12) return false; // longueur + type + CRC
            long longueur = Integer.toUnsignedLong(buffer.getInt(position));
            if (longueur > image.length - position - 12L) return false; // bloc tronqué
            int debutDonnees = position + 8;
            int fin = debutDonnees + (int) longueur;
            String type = new String(image, position + 4, 4, StandardCharsets.US_ASCII);

            if (!BLOCS_AUTORISES.contains(type)) return false;
            if ("iCCP".equals(type) && longueur > ICCP_MAX_OCTETS) return false;
            if (premier != "IHDR".equals(type)) return false; // IHDR une seule fois, en premier

            CRC32 crc = new CRC32();
            crc.update(image, position + 4, 4 + (int) longueur);
            if ((int) crc.getValue() != buffer.getInt(fin)) return false;

            if (premier && !enTeteValide(buffer, debutDonnees, longueur)) return false;
            if ("IDAT".equals(type)) idat = true;

            position = fin + 4;
            premier = false;

            if ("IEND".equals(type)) {
                return idat && longueur == 0 && position == image.length; // rien après IEND
            }
        }
        return false; // pas de IEND
    }

    private static boolean enTeteValide(ByteBuffer buffer, int debut, long longueur) {
        if (longueur != 13) return false;
        int largeur = buffer.getInt(debut);
        int hauteur = buffer.getInt(debut + 4);
        int profondeur = buffer.get(debut + 8);
        int typeCouleur = buffer.get(debut + 9);
        return largeur > 0 && largeur <= LARGEUR_MAX
                && hauteur > 0 && hauteur <= HAUTEUR_MAX
                && profondeur == 8
                && TYPES_COULEUR.contains(typeCouleur);
    }
}
