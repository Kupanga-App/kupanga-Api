package com.kupanga.api.stockage.service;

import com.kupanga.api.stockage.image.PhotoPreparee;
import com.kupanga.api.stockage.image.PhotoStockee;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * Stockage des fichiers dans un serveur compatible S3 (S1) : aucune dépendance à un serveur en particulier.
 * Les buckets et leurs droits sont préparés au démarrage ({@code PreparationBuckets}), jamais à l'envoi.
 * <p>
 * S2 : la base ne garde que des <b>clés</b> ; l'URL (publique ou présignée) est construite à la lecture,
 * pour qu'un changement de domaine ou de serveur ne casse aucun fichier.
 */
public interface StockageService {

    /**
     * R1 : envoie la première version d'une photo déjà préparée (ex. photo de profil, 400 px).
     * @param photo photo contrôlée et réduite ({@link PhotoPreparee#pourBucketPublic})
     * @param bucketName nom du bucket
     * @return la clé de l'objet créé
     */
    String envoyerImage(PhotoPreparee photo, String bucketName);

    /**
     * R1 : envoie une photo de bien préparée en deux versions (1600 px puis vignette de 400 px).
     * @param photo photo préparée avec deux côtés, le grand d'abord
     * @param bucketName nom du bucket
     * @return les clés des deux versions
     */
    PhotoStockee envoyerPhotoAvecVignette(PhotoPreparee photo, String bucketName);

    /**
     * URL publique d'un objet d'un bucket public (photos des biens), sans signature.
     * @return l'URL, ou {@code null} si la clé est vide
     */
    String urlPublique(String bucketName, String cle);

    /**
     * Uploader un pdf dans un bucket <b>privé</b> (contrats, EDL, quittances — P0-7).
     * @param pdf le pdf
     * @param originalName nom du pdf
     * @param bucketName nom du bucket
     * @return la clé de l'objet dans le bucket (jamais une URL publique)
     */
    String uploadPdf(byte[] pdf, String originalName , String bucketName);

    /**
     * Génère une URL présignée de lecture, valable {@code DUREE_URL_PRESIGNEE_MINUTES} minutes.
     * @param bucketName nom du bucket
     * @param cle clé de l'objet
     * @return l'URL présignée, ou {@code null} si la clé est vide
     */
    String urlPresignee(String bucketName, String cle);

    /**
     * URL présignée de lecture d'une durée choisie (S2 : photos de profil, 24 h).
     * @return l'URL présignée, ou {@code null} si la clé est vide
     */
    String urlPresignee(String bucketName, String cle, Duration duree);

    /**
     * Télécharge un objet (ex. PDF à joindre à un e-mail).
     * @param bucketName nom du bucket
     * @param cle clé de l'objet
     * @return le contenu du fichier
     */
    byte[] telecharger(String bucketName, String cle);

    /**
     * C8-bis : envoie une image (contrôlée au contenu, B5) dans un bucket <b>privé</b>, réduite à 1600 px et
     * sans métadonnées EXIF/GPS (R1).
     *
     * @return la clé de l'objet (lue ensuite par URL présignée, jamais par une URL publique)
     */
    String uploadImagePrivee(MultipartFile file, String bucketName);

    /** Supprime un objet par sa clé ; une erreur est journalisée sans être propagée. */
    void supprimer(String bucketName, String cle);
}
