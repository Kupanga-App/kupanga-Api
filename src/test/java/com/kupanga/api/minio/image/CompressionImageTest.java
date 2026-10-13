package com.kupanga.api.minio.image;

import com.kupanga.api.exception.business.KupangaBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R1 : réencodage JPEG, redimensionnement, orientation EXIF appliquée puis métadonnées retirées.
 */
@DisplayName("Tests unitaires — CompressionImage / OrientationExif (R1)")
class CompressionImageTest {

    @Test
    @DisplayName("Grande photo : 1600 px et vignette de 400 px, proportions gardées, en JPEG")
    void grandePhoto_deuxVersions() {
        List<byte[]> versions = CompressionImage.versJpeg(ImagesDeTest.jpeg(4000, 3000), 1600, 400);

        BufferedImage grande = ImagesDeTest.lire(versions.get(0));
        BufferedImage vignette = ImagesDeTest.lire(versions.get(1));
        assertThat(grande.getWidth()).isEqualTo(1600);
        assertThat(grande.getHeight()).isEqualTo(1200);
        assertThat(vignette.getWidth()).isEqualTo(400);
        assertThat(vignette.getHeight()).isEqualTo(300);
        assertThat(FormatImage.detecter(versions.get(0))).contains(FormatImage.JPEG);
        assertThat(versions.get(1).length).isLessThan(versions.get(0).length);
    }

    @Test
    @DisplayName("Photo en hauteur : le grand côté (hauteur) est borné")
    void photoEnHauteur() {
        BufferedImage grande = ImagesDeTest.lire(CompressionImage.versJpeg(ImagesDeTest.png(900, 3000), 1600).get(0));

        assertThat(grande.getHeight()).isEqualTo(1600);
        assertThat(grande.getWidth()).isEqualTo(480);
    }

    @Test
    @DisplayName("Petite photo : jamais agrandie")
    void petitePhoto_pasAgrandie() {
        BufferedImage image = ImagesDeTest.lire(CompressionImage.versJpeg(ImagesDeTest.gif(300, 200), 1600).get(0));

        assertThat(image.getWidth()).isEqualTo(300);
        assertThat(image.getHeight()).isEqualTo(200);
    }

    @Test
    @DisplayName("BMP et TIFF (scans, captures) : réencodés en JPEG comme les autres formats")
    void bmpEtTiff_enJpeg() {
        for (byte[] source : new byte[][]{ImagesDeTest.bmp(120, 80), ImagesDeTest.tiff(120, 80)}) {
            byte[] jpeg = CompressionImage.versJpeg(source, 1600).get(0);
            assertThat(FormatImage.detecter(jpeg)).contains(FormatImage.JPEG);
            BufferedImage image = ImagesDeTest.lire(jpeg);
            assertThat(image.getWidth()).isEqualTo(120);
            assertThat(image.getHeight()).isEqualTo(80);
        }
    }

    @Test
    @DisplayName("PNG transparent : fond blanc (le JPEG n'a pas de transparence, pas de fond noir)")
    void transparence_fondBlanc() {
        BufferedImage image = ImagesDeTest.lire(CompressionImage.versJpeg(ImagesDeTest.pngTransparent(50, 50), 400).get(0));

        Color centre = new Color(image.getRGB(25, 25));
        assertThat(centre.getRed()).isGreaterThan(240);
        assertThat(centre.getGreen()).isGreaterThan(240);
        assertThat(centre.getBlue()).isGreaterThan(240);
    }

    @Test
    @DisplayName("EXIF (position GPS) retiré ; orientation 6 appliquée : la photo couchée est redressée")
    void exif_retire_orientationAppliquee() {
        // 400 × 200 : rouge à gauche, bleu à droite ; orientation 6 = tourner de 90° dans le sens horaire
        byte[] avecExif = ImagesDeTest.jpegAvecExif(400, 200, 6, true);
        assertThat(ImagesDeTest.contient(avecExif, "GPS-KINSHASA")).isTrue();

        byte[] resultat = CompressionImage.versJpeg(avecExif, 1600).get(0);

        assertThat(ImagesDeTest.contient(resultat, "GPS-KINSHASA")).isFalse();
        assertThat(ImagesDeTest.contient(resultat, "Exif")).isFalse();
        BufferedImage image = ImagesDeTest.lire(resultat);
        assertThat(image.getWidth()).isEqualTo(200);
        assertThat(image.getHeight()).isEqualTo(400);
        // Après rotation horaire, la gauche (rouge) passe en haut et la droite (bleu) en bas
        assertThat(new Color(image.getRGB(100, 50)).getRed()).isGreaterThan(200);
        assertThat(new Color(image.getRGB(100, 350)).getBlue()).isGreaterThan(200);
    }

    @ParameterizedTest(name = "orientation {0}")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    @DisplayName("Orientation EXIF lue en grand et en petit boutiste")
    void orientationExif_lue(int orientation) {
        assertThat(OrientationExif.lire(ImagesDeTest.jpegAvecExif(20, 10, orientation, true))).isEqualTo(orientation);
        assertThat(OrientationExif.lire(ImagesDeTest.jpegAvecExif(20, 10, orientation, false))).isEqualTo(orientation);
    }

    @Test
    @DisplayName("Orientation : absente, invalide ou fichier tronqué → 1 (aucune rotation), jamais d'exception")
    void orientationExif_robuste() {
        assertThat(OrientationExif.lire(ImagesDeTest.jpeg(20, 10))).isEqualTo(1);
        assertThat(OrientationExif.lire(ImagesDeTest.jpegAvecExif(20, 10, 42, true))).isEqualTo(1);
        assertThat(OrientationExif.lire(ImagesDeTest.png(20, 10))).isEqualTo(1);
        assertThat(OrientationExif.lire(null)).isEqualTo(1);
        byte[] avecExif = ImagesDeTest.jpegAvecExif(20, 10, 6, true);
        for (int taille = 0; taille < 60; taille++) {
            assertThat(OrientationExif.lire(Arrays.copyOf(avecExif, taille))).isBetween(1, 8);
        }
    }

    @Test
    @DisplayName("Orientations 2 à 8 : dimensions échangées à partir de 5")
    void orienter_dimensions() {
        BufferedImage source = ImagesDeTest.deuxCouleurs(40, 20, BufferedImage.TYPE_INT_RGB);
        for (int orientation = 1; orientation <= 8; orientation++) {
            BufferedImage oriente = CompressionImage.orienter(source, orientation);
            boolean tourne = orientation >= 5;
            assertThat(oriente.getWidth()).isEqualTo(tourne ? 20 : 40);
            assertThat(oriente.getHeight()).isEqualTo(tourne ? 40 : 20);
        }
    }

    @Test
    @DisplayName("Bombe de décompression : plus de 100 Mpx annoncés → 400 sans décoder les pixels")
    void tropDePixels_refuse() {
        byte[] enorme = ImagesDeTest.pngEnteteSeul(20_000, 20_000);

        assertThatThrownBy(() -> CompressionImage.verifierDimensions(enorme))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> CompressionImage.versJpeg(enorme, 1600))
                .isInstanceOf(KupangaBusinessException.class);
    }

    @Test
    @DisplayName("Revue R1 : PNG de 30 000 000 × 3 px (350 Ko, 90 Mpx) → 400 sans décodage (plus d'OutOfMemoryError)")
    void pngTresLarge_refuseSansDecodage() {
        byte[] large = ImagesDeTest.pngEnteteSeul(30_000_000, 3);

        assertThatThrownBy(() -> CompressionImage.versJpeg(large, 1600))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        // Côté borné même sous le total de pixels
        assertThatThrownBy(() -> CompressionImage.verifierDimensions(ImagesDeTest.pngEnteteSeul(12_001, 10)))
                .isInstanceOf(KupangaBusinessException.class);
    }

    @Test
    @DisplayName("Revue R1 : JPEG progressif refusé au-delà de 4 Mpx, accepté en dessous")
    void jpegProgressif_borne() {
        byte[] petit = ImagesDeTest.jpegProgressif(300, 200);
        byte[] grand = ImagesDeTest.jpegProgressif(2100, 2000);
        assertThat(EnteteJpeg.estBaseline(petit)).isFalse();
        assertThat(EnteteJpeg.estBaseline(ImagesDeTest.jpeg(300, 200))).isTrue();

        assertThat(CompressionImage.versJpeg(petit, 400)).hasSize(1);
        assertThatThrownBy(() -> CompressionImage.verifierDimensions(grand))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Revue R1 (2e tour) : octet parasite avant SOF2 ou longueur invalide → refusé au-delà de 4 Mpx")
    void jpegProgressif_contournement_refuse() {
        byte[] grand = ImagesDeTest.jpegProgressif(2100, 2000);
        int sof = sof(grand);
        byte[] parasite = new byte[grand.length + 1];
        System.arraycopy(grand, 0, parasite, 0, sof);
        parasite[sof] = 0x00;
        System.arraycopy(grand, sof, parasite, sof + 1, grand.length - sof);
        byte[] baselineParasite = ImagesDeTest.jpeg(2100, 2000);
        byte[] longueurInvalide = baselineParasite.clone();
        longueurInvalide[4] = 0; // longueur du premier segment (après SOI) invalide → prudence
        longueurInvalide[5] = 1;

        assertThatThrownBy(() -> CompressionImage.verifierDimensions(parasite))
                .isInstanceOf(KupangaBusinessException.class);
        assertThat(EnteteJpeg.estBaseline(longueurInvalide)).isFalse();
        // Un grand JPEG baseline sans anomalie reste accepté
        CompressionImage.verifierDimensions(baselineParasite);
    }

    /** Position du premier marqueur de trame (SOF0 ou SOF2). */
    private static int sof(byte[] jpeg) {
        for (int i = 2; i + 1 < jpeg.length; i++) {
            int m = jpeg[i + 1] & 0xFF;
            if ((jpeg[i] & 0xFF) == 0xFF && (m == 0xC0 || m == 0xC2)) return i;
        }
        throw new IllegalStateException("pas de SOF");
    }

    @Test
    @DisplayName("En-tête lisible mais données endommagées → 400 au décodage")
    void donneesEndommagees_400() {
        byte[] png = ImagesDeTest.pngEndommage(40, 30);
        CompressionImage.verifierDimensions(png); // l'en-tête passe

        assertThatThrownBy(() -> CompressionImage.versJpeg(png, 400))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Photo endommagée (en-tête seul, données tronquées) → 400, pas de 500")
    void photoEndommagee_400() {
        byte[] jpeg = ImagesDeTest.jpeg(200, 100);
        byte[] tronque = Arrays.copyOf(jpeg, 20);
        byte[] entetePng = Arrays.copyOf(ImagesDeTest.png(10, 10), 12);

        for (byte[] contenu : List.of(tronque, entetePng)) {
            assertThatThrownBy(() -> CompressionImage.versJpeg(contenu, 1600))
                    .isInstanceOfSatisfying(KupangaBusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }
}
