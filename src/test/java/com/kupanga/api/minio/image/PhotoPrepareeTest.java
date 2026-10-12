package com.kupanga.api.minio.image;

import com.kupanga.api.exception.business.KupangaBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R1 (revue) : préparation des photos selon le bucket ; aucune position GPS ne reste dans une photo publique.
 */
@DisplayName("Tests unitaires — PhotoPreparee / NettoyageWebp (R1)")
class PhotoPrepareeTest {

    private static MockMultipartFile fichier(byte[] contenu) {
        return new MockMultipartFile("files", "photo.bin", "application/octet-stream", contenu);
    }

    @Test
    @DisplayName("JPEG : une version JPEG par côté, la grande d'abord")
    void jpeg_deuxVersions() {
        PhotoPreparee photo = PhotoPreparee.pourBucketPublic(fichier(ImagesDeTest.jpeg(2000, 1000)), 1600, 400);

        assertThat(photo.versions()).hasSize(2);
        assertThat(photo.versions()).allMatch(v -> v.format() == FormatImage.JPEG);
        assertThat(ImagesDeTest.lire(photo.versions().get(0).contenu()).getWidth()).isEqualTo(1600);
        assertThat(ImagesDeTest.lire(photo.versions().get(1).contenu()).getWidth()).isEqualTo(400);
    }

    @Test
    @DisplayName("WEBP : blocs EXIF et XMP retirés, indicateurs VP8X effacés, image conservée")
    void webp_nettoye() {
        byte[] webp = ImagesDeTest.webpAvecExif();

        PhotoPreparee photo = PhotoPreparee.pourBucketPublic(fichier(webp), 1600, 400);

        byte[] nettoye = photo.versions().get(0).contenu();
        assertThat(photo.versions().get(1)).isSameAs(photo.versions().get(0));
        assertThat(photo.versions().get(0).format()).isEqualTo(FormatImage.WEBP);
        assertThat(ImagesDeTest.contient(nettoye, "GPS-KINSHASA")).isFalse();
        assertThat(ImagesDeTest.contient(nettoye, "GPS-XMP")).isFalse();
        assertThat(ImagesDeTest.contient(nettoye, "VP8 ")).isTrue();
        assertThat(nettoye[20] & 0x0C).isZero(); // indicateurs du bloc VP8X (octet 8 du bloc)
        long tailleRiff = (nettoye[4] & 0xFF) | (nettoye[5] & 0xFF) << 8 | (nettoye[6] & 0xFF) << 16;
        assertThat(tailleRiff).isEqualTo(nettoye.length - 8);
        assertThat(FormatImage.detecter(nettoye)).contains(FormatImage.WEBP);
    }

    @Test
    @DisplayName("WEBP à la structure invalide (bloc plus long que le fichier) → 400")
    void webp_invalide_400() {
        byte[] webp = ImagesDeTest.webpAvecExif();
        webp[16] = (byte) 0xFF; // taille du premier bloc démesurée
        webp[17] = (byte) 0xFF;

        assertThatThrownBy(() -> PhotoPreparee.pourBucketPublic(fichier(webp), 400))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("AVIF / HEIC : refusés en 415 pour une photo publique (EXIF impossible à retirer), admis en privé")
    void avifHeic_publicRefuse_priveAdmis() {
        byte[] avif = ValidationImageTest.completer(ValidationImageTest.ftyp("avif", "mif1", "miaf"));
        byte[] heic = ValidationImageTest.completer(ValidationImageTest.ftyp("heic", "mif1", "heic"));

        for (byte[] contenu : new byte[][]{avif, heic}) {
            assertThatThrownBy(() -> PhotoPreparee.pourBucketPublic(fichier(contenu), 1600, 400))
                    .isInstanceOfSatisfying(KupangaBusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        }
        PhotoPreparee recu = PhotoPreparee.pourBucketPrive(fichier(avif), 1600);
        assertThat(recu.versions().get(0).format()).isEqualTo(FormatImage.AVIF);
    }
}
