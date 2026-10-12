package com.kupanga.api.minio.service;

import com.kupanga.api.minio.image.PhotoPreparee;
import com.kupanga.api.minio.image.PhotoStockee;
import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    /**
     * Créer le bucket s'il n'existe pas et l'initialise à public.
     * @param bucketName nom du bucket.
     * @param publicRead booléen le spécifiant public
     */
    void createBucketIfNotExists(String bucketName, boolean publicRead);

    /**
     * R1 : envoie dans un bucket public la première version d'une photo déjà préparée (ex. photo de profil).
     * @param photo photo contrôlée et réduite ({@link PhotoPreparee#pourBucketPublic})
     * @param bucketName nom du bucket
     * @return Url de l'image dans minio.
     */
    String envoyerImagePublique(PhotoPreparee photo, String bucketName);

    /**
     * R1 : envoie dans un bucket public une photo de bien préparée en deux versions (1600 px puis vignette de 400 px).
     * @param photo photo préparée avec deux côtés, le grand d'abord
     * @param bucketName nom du bucket
     * @return les URL des deux versions
     */
    PhotoStockee envoyerPhotoAvecVignette(PhotoPreparee photo, String bucketName);

    /**
     * Indique si une URL désigne un objet de ce bucket sur notre MinIO (ex. avatar choisi par le front),
     * et non une adresse externe (pixel de suivi, contenu non contrôlé).
     * @param url URL reçue du client
     * @param bucketName bucket attendu
     * @return {@code true} si l'URL est {@code <url MinIO>/<bucket>/<nom simple>}
     */
    boolean estUrlDuBucket(String url, String bucketName);

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
     * Télécharge un objet (ex. PDF à joindre à un e-mail).
     * @param bucketName nom du bucket
     * @param cle clé de l'objet
     * @return le contenu du fichier
     */
    byte[] telecharger(String bucketName, String cle);

    /**
     * B12 : supprime l'objet désigné par une URL de ce bucket (ex. photo de profil d'un compte supprimé).
     * Sans effet si l'URL n'est pas celle d'un objet de ce bucket ; une erreur MinIO est journalisée, pas propagée.
     * @param url URL publique de l'objet
     * @param bucketName bucket attendu
     */
    void supprimerParUrl(String url, String bucketName);

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
