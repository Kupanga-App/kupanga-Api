package com.kupanga.api.minio.image;

import com.kupanga.api.exception.business.KupangaBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Contrôle des photos envoyées (B5) : format reconnu au contenu réel, taille, nombre de photos par bien.
 */
public final class ValidationImage {

    /** Taille maximale d'une photo (alignée sur {@code spring.servlet.multipart.max-file-size}). */
    public static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024;

    /** Nombre maximal de photos par bien (décision 2026-10-09). */
    public static final int NOMBRE_MAX_PHOTOS_BIEN = 20;

    static final String FORMATS_ACCEPTES = "JPEG, PNG, WEBP, AVIF, GIF, HEIC";

    private ValidationImage() {
    }

    /**
     * Vérifie une photo et renvoie son format réel.
     *
     * @throws KupangaBusinessException 400 (vide ou illisible), 413 (trop lourde), 415 (format non accepté)
     */
    public static FormatImage verifier(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new KupangaBusinessException("Le fichier envoyé est vide", HttpStatus.BAD_REQUEST);
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new KupangaBusinessException("Photo trop volumineuse (10 Mo maximum)", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        return FormatImage.detecter(lireEntete(fichier))
                .orElseThrow(() -> new KupangaBusinessException(
                        "Format de photo non accepté (formats acceptés : " + FORMATS_ACCEPTES + ")",
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    /** Vérifie toutes les photos d'un bien avant tout enregistrement (aucun envoi partiel vers MinIO). */
    public static void verifierPhotosBien(List<MultipartFile> fichiers) {
        if (fichiers.size() > NOMBRE_MAX_PHOTOS_BIEN) {
            throw new KupangaBusinessException(
                    "Trop de photos (" + NOMBRE_MAX_PHOTOS_BIEN + " maximum par bien)", HttpStatus.BAD_REQUEST);
        }
        fichiers.forEach(ValidationImage::verifier);
    }

    private static byte[] lireEntete(MultipartFile fichier) {
        try (InputStream in = fichier.getInputStream()) {
            return in.readNBytes(FormatImage.TAILLE_ENTETE);
        } catch (IOException e) {
            throw new KupangaBusinessException("Fichier illisible", HttpStatus.BAD_REQUEST);
        }
    }
}
