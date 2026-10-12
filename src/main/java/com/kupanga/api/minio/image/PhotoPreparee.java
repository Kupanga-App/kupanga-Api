package com.kupanga.api.minio.image;

import com.kupanga.api.exception.business.KupangaBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * R1 : photo contrôlée (B5) et préparée <b>avant</b> tout enregistrement, une version par grand côté demandé.
 * <ul>
 *   <li>JPEG, PNG, GIF : réencodés en JPEG réduit, sans métadonnées ;</li>
 *   <li>WEBP : non décodable en Java pur, stocké tel quel sans ses blocs EXIF/XMP ;</li>
 *   <li>AVIF, HEIC, HEIF : métadonnées impossibles à retirer sans dépendance native → refusés (415) pour une
 *       photo publique (décision 2026-10-12 ; le front convertit HEIC et réencode AVIF), stockés tels quels
 *       dans un bucket privé (reçu).</li>
 * </ul>
 * Pour un format non réduit, toutes les versions sont le même objet (une seule copie envoyée).
 *
 * @param versions une version par grand côté, dans l'ordre demandé
 */
public record PhotoPreparee(List<FichierImage> versions) {

    private static final Set<FormatImage> SANS_NETTOYAGE = Set.of(FormatImage.AVIF, FormatImage.HEIC, FormatImage.HEIF);

    /** Photo destinée à un bucket public (photos des biens, photo de profil). */
    public static PhotoPreparee pourBucketPublic(MultipartFile fichier, int... cotes) {
        return preparer(fichier, true, cotes);
    }

    /** Photo destinée à un bucket privé (reçu de paiement, lu par URL présignée). */
    public static PhotoPreparee pourBucketPrive(MultipartFile fichier, int... cotes) {
        return preparer(fichier, false, cotes);
    }

    private static PhotoPreparee preparer(MultipartFile fichier, boolean publique, int... cotes) {
        FormatImage format = ValidationImage.verifier(fichier);
        byte[] contenu = lire(fichier);
        if (CompressionImage.estDecodable(format)) {
            return new PhotoPreparee(CompressionImage.versJpeg(contenu, cotes).stream()
                    .map(jpeg -> new FichierImage(jpeg, FormatImage.JPEG))
                    .toList());
        }
        if (format == FormatImage.WEBP) {
            contenu = NettoyageWebp.nettoyer(contenu);
        } else if (publique && SANS_NETTOYAGE.contains(format)) {
            throw new KupangaBusinessException(
                    "Format de photo non accepté (formats acceptés : JPEG, PNG, WEBP, GIF)",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return new PhotoPreparee(Collections.nCopies(cotes.length, new FichierImage(contenu, format)));
    }

    private static byte[] lire(MultipartFile fichier) {
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new KupangaBusinessException("Fichier illisible", HttpStatus.BAD_REQUEST);
        }
    }
}
