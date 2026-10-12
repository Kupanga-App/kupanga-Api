package com.kupanga.api.minio.image;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * R1 : vraies images pour les tests (depuis R1, les dimensions sont lues et les pixels décodés : un en-tête seul
 * ne suffit plus).
 */
public final class ImagesDeTest {

    private ImagesDeTest() {
    }

    /** JPEG uni de la taille demandée, moitié gauche rouge et moitié droite bleue. */
    public static byte[] jpeg(int largeur, int hauteur) {
        return encoder(deuxCouleurs(largeur, hauteur, BufferedImage.TYPE_INT_RGB), "jpeg");
    }

    public static byte[] png(int largeur, int hauteur) {
        return encoder(deuxCouleurs(largeur, hauteur, BufferedImage.TYPE_INT_RGB), "png");
    }

    public static byte[] gif(int largeur, int hauteur) {
        return encoder(deuxCouleurs(largeur, hauteur, BufferedImage.TYPE_INT_RGB), "gif");
    }

    /** PNG entièrement transparent. */
    public static byte[] pngTransparent(int largeur, int hauteur) {
        return encoder(new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_ARGB), "png");
    }

    /**
     * JPEG avec un segment EXIF (APP1) : orientation et position GPS fictive (texte repérable « GPS-KINSHASA »).
     *
     * @param grandBoutiste ordre des octets « MM » (sinon « II »)
     */
    public static byte[] jpegAvecExif(int largeur, int hauteur, int orientation, boolean grandBoutiste) {
        byte[] jpeg = jpeg(largeur, hauteur);
        ByteBuffer tiff = ByteBuffer.allocate(26 + 12);
        tiff.order(grandBoutiste ? java.nio.ByteOrder.BIG_ENDIAN : java.nio.ByteOrder.LITTLE_ENDIAN);
        tiff.put(grandBoutiste ? (byte) 'M' : (byte) 'I').put(grandBoutiste ? (byte) 'M' : (byte) 'I');
        tiff.putShort((short) 42).putInt(8);           // en-tête TIFF, IFD0 à l'octet 8
        tiff.putShort((short) 1);                       // une entrée
        tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0);
        tiff.putInt(0);                                  // pas d'IFD suivant
        tiff.put("GPS-KINSHASA".getBytes(StandardCharsets.ISO_8859_1));
        byte[] exif = concat("Exif\0\0".getBytes(StandardCharsets.ISO_8859_1), tiff.array());
        int longueur = exif.length + 2;
        byte[] app1 = concat(new byte[]{(byte) 0xFF, (byte) 0xE1, (byte) (longueur >> 8), (byte) longueur}, exif);
        // Juste après SOI (FF D8)
        return concat(new byte[]{jpeg[0], jpeg[1]}, app1, java.util.Arrays.copyOfRange(jpeg, 2, jpeg.length));
    }

    /** En-tête PNG valide (IHDR avec CRC) annonçant des dimensions énormes, sans données d'image. */
    public static byte[] pngEnteteSeul(int largeur, int hauteur) {
        ByteBuffer ihdr = ByteBuffer.allocate(17);
        ihdr.put("IHDR".getBytes(StandardCharsets.ISO_8859_1)).putInt(largeur).putInt(hauteur)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        CRC32 crc = new CRC32();
        crc.update(ihdr.array());
        ByteBuffer png = ByteBuffer.allocate(8 + 4 + 17 + 4);
        png.put(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}).putInt(13).put(ihdr.array())
                .putInt((int) crc.getValue());
        return png.array();
    }

    /** PNG à l'en-tête valide (dimensions lisibles) mais aux données d'image endommagées : décodage impossible. */
    public static byte[] pngEndommage(int largeur, int hauteur) {
        byte[] png = png(largeur, hauteur);
        int idat = new String(png, StandardCharsets.ISO_8859_1).indexOf("IDAT");
        for (int i = idat + 4; i < idat + 14 && i < png.length; i++) png[i] = (byte) 0xAB;
        return png;
    }

    /** JPEG progressif (marqueur SOF2). */
    public static byte[] jpegProgressif(int largeur, int hauteur) {
        javax.imageio.ImageWriter ecrivain = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (javax.imageio.stream.ImageOutputStream flux = ImageIO.createImageOutputStream(sortie)) {
            ecrivain.setOutput(flux);
            javax.imageio.ImageWriteParam parametres = ecrivain.getDefaultWriteParam();
            parametres.setProgressiveMode(javax.imageio.ImageWriteParam.MODE_DEFAULT);
            ecrivain.write(null, new javax.imageio.IIOImage(
                    new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB), null, null), parametres);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            ecrivain.dispose();
        }
        return sortie.toByteArray();
    }

    /** WEBP minimal (bloc VP8X avec indicateurs EXIF et XMP, bloc VP8 factice, blocs EXIF et XMP avec un texte GPS). */
    public static byte[] webpAvecExif() {
        ByteArrayOutputStream corps = new ByteArrayOutputStream();
        corps.writeBytes(bloc("VP8X", new byte[]{0x0C, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        corps.writeBytes(bloc("VP8 ", new byte[]{1, 2, 3, 4, 5}));
        corps.writeBytes(bloc("EXIF", "GPS-KINSHASA".getBytes(StandardCharsets.ISO_8859_1)));
        corps.writeBytes(bloc("XMP ", "<x>GPS-XMP</x>".getBytes(StandardCharsets.ISO_8859_1)));
        byte[] c = corps.toByteArray();
        ByteBuffer riff = ByteBuffer.allocate(12 + c.length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        riff.put("RIFF".getBytes(StandardCharsets.ISO_8859_1)).putInt(4 + c.length)
                .put("WEBP".getBytes(StandardCharsets.ISO_8859_1)).put(c);
        return riff.array();
    }

    private static byte[] bloc(String type, byte[] donnees) {
        int remplissage = donnees.length & 1;
        ByteBuffer b = ByteBuffer.allocate(8 + donnees.length + remplissage).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put(type.getBytes(StandardCharsets.ISO_8859_1)).putInt(donnees.length).put(donnees);
        return b.array();
    }

    public static BufferedImage lire(byte[] contenu) {
        try {
            return ImageIO.read(new ByteArrayInputStream(contenu));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static boolean contient(byte[] contenu, String texte) {
        return new String(contenu, StandardCharsets.ISO_8859_1).contains(texte);
    }

    static BufferedImage deuxCouleurs(int largeur, int hauteur, int type) {
        BufferedImage image = new BufferedImage(largeur, hauteur, type);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, largeur / 2, hauteur);
        g.setColor(Color.BLUE);
        g.fillRect(largeur / 2, 0, largeur - largeur / 2, hauteur);
        g.dispose();
        return image;
    }

    private static byte[] encoder(BufferedImage image, String format) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, format, sortie)) throw new IllegalStateException("Pas d'encodeur " + format);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }

    private static byte[] concat(byte[]... parties) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        for (byte[] partie : parties) sortie.writeBytes(partie);
        return sortie.toByteArray();
    }
}
