package com.kupanga.api.minio.image;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Formats de photo acceptés (B5), reconnus à la signature du contenu et jamais à l'extension ni au
 * {@code Content-Type} envoyés par le client. Formats des smartphones (décision 2026-10-09) ; les photos
 * HEIC/HEIF sont converties en JPEG par le front avant l'envoi, celles qui arrivent quand même sont stockées telles quelles.
 */
@Getter
public enum FormatImage {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp"),
    AVIF("image/avif", "avif"),
    HEIC("image/heic", "heic"),
    HEIF("image/heif", "heif");

    /** Octets lus en tête de fichier pour reconnaître le format (boîte {@code ftyp} ISO BMFF comprise). */
    public static final int TAILLE_ENTETE = 64;

    private static final byte[] SIGNATURE_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] SIGNATURE_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final Set<String> MARQUES_AVIF = Set.of("avif", "avis");
    private static final Set<String> MARQUES_HEIC = Set.of("heic", "heix", "hevc", "hevx", "heim", "heis");
    private static final Set<String> MARQUES_HEIF = Set.of("mif1", "msf1");

    private final String typeMime;
    private final String extension;

    FormatImage(String typeMime, String extension) {
        this.typeMime = typeMime;
        this.extension = extension;
    }

    /** Format reconnu d'après les premiers octets du fichier, ou vide (exécutable, HTML, SVG, PDF…). */
    public static Optional<FormatImage> detecter(byte[] entete) {
        if (entete == null) return Optional.empty();
        if (commencePar(entete, SIGNATURE_JPEG)) return Optional.of(JPEG);
        if (commencePar(entete, SIGNATURE_PNG)) return Optional.of(PNG);
        String debut = texte(entete, 0, 6);
        if (debut.equals("GIF87a") || debut.equals("GIF89a")) return Optional.of(GIF);
        if (texte(entete, 0, 4).equals("RIFF") && texte(entete, 8, 4).equals("WEBP")) return Optional.of(WEBP);
        if (texte(entete, 4, 4).equals("ftyp")) return formatIsoBmff(entete);
        return Optional.empty();
    }

    /** AVIF / HEIC / HEIF : marque principale (octets 8-11), puis marques compatibles de la boîte {@code ftyp}. */
    private static Optional<FormatImage> formatIsoBmff(byte[] entete) {
        int finBoite = Math.min(entete.length, Math.max(16, lireEntier(entete)));
        Optional<FormatImage> principal = formatDeMarque(texte(entete, 8, 4));
        if (principal.isPresent() && principal.get() != HEIF) return principal;
        // Marque générique (mif1, msf1) : une marque compatible précise le codec
        for (int i = 16; i + 4 <= finBoite; i += 4) {
            Optional<FormatImage> compatible = formatDeMarque(texte(entete, i, 4));
            if (compatible.isPresent() && compatible.get() != HEIF) return compatible;
        }
        return principal;
    }

    private static Optional<FormatImage> formatDeMarque(String marque) {
        if (MARQUES_AVIF.contains(marque)) return Optional.of(AVIF);
        if (MARQUES_HEIC.contains(marque)) return Optional.of(HEIC);
        if (MARQUES_HEIF.contains(marque)) return Optional.of(HEIF);
        return Optional.empty();
    }

    private static boolean commencePar(byte[] donnees, byte[] signature) {
        return donnees.length >= signature.length
                && Arrays.equals(donnees, 0, signature.length, signature, 0, signature.length);
    }

    private static String texte(byte[] donnees, int debut, int longueur) {
        if (donnees.length < debut + longueur) return "";
        return new String(donnees, debut, longueur, StandardCharsets.ISO_8859_1);
    }

    private static int lireEntier(byte[] donnees) {
        return ((donnees[0] & 0xFF) << 24) | ((donnees[1] & 0xFF) << 16)
                | ((donnees[2] & 0xFF) << 8) | (donnees[3] & 0xFF);
    }
}
