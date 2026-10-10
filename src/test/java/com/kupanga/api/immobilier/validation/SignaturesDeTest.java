package com.kupanga.api.immobilier.validation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

/** Signatures PNG réelles en base64 (sans préfixe), comme celles du pad de signature du front. */
public final class SignaturesDeTest {

    private SignaturesDeTest() {
    }

    public static String png(int largeur, int hauteur) {
        BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(largeur / 2, hauteur / 2, 0xFF000000);
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", sortie);
            return Base64.getEncoder().encodeToString(sortie.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Signature au format du pad du front (480 × 140). */
    public static String signatureValide() {
        return png(480, 140);
    }
}
