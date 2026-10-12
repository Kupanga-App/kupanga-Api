package com.kupanga.api.minio.image;

import com.kupanga.api.exception.business.KupangaBusinessException;
import org.springframework.http.HttpStatus;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * R1 : photos réencodées en JPEG et redimensionnées à l'envoi (réseau mobile lent et cher au Congo).
 * Java pur (ImageIO) : aucune bibliothèque native, compatible avec l'image Docker Alpine. Le réencodage retire
 * toutes les métadonnées (EXIF, position GPS) ; l'orientation EXIF est appliquée aux pixels avant.
 * <p>
 * WEBP, AVIF, HEIC et HEIF ne sont pas décodables sans dépendance native (cf. {@link PhotoPreparee}).
 */
public final class CompressionImage {

    /** Grand côté de la version affichée en plein écran (photos des biens, reçus). */
    public static final int COTE_GRANDE = 1600;

    /** Grand côté de la vignette (listes, cartes) et de la photo de profil. */
    public static final int COTE_VIGNETTE = 400;

    /**
     * Bornes lues avant tout décodage (bombes de décompression) : 50 Mpx au total (capteur de smartphone : 48 Mpx)
     * et 12 000 px par côté (revue R1 : le lecteur PNG alloue des tampons de la largeur d'une ligne, un PNG de
     * 30 000 000 × 3 px de 350 Ko épuisait la mémoire malgré le sous-échantillonnage).
     */
    static final long PIXELS_MAX = 50_000_000L;
    static final int COTE_SOURCE_MAX = 12_000;

    /**
     * JPEG progressif : la libjpeg du JDK garde tous les coefficients en mémoire native et parcourt l'image à
     * chaque scan. Le front et les appareils photo produisent du JPEG « baseline » : progressif admis jusqu'à 4 Mpx.
     */
    static final long PIXELS_MAX_JPEG_PROGRESSIF = 4_000_000L;

    /** Décodages simultanés (CPU et mémoire bornés quel que soit le nombre de requêtes). */
    private static final Semaphore DECODAGES = new Semaphore(2, true);
    private static final long ATTENTE_DECODAGE_SECONDES = 20;

    private static final float QUALITE_JPEG = 0.82f;

    private static final Set<FormatImage> DECODABLES = Set.of(FormatImage.JPEG, FormatImage.PNG, FormatImage.GIF);

    private CompressionImage() {
    }

    /** Formats que Java sait décoder sans dépendance native. */
    public static boolean estDecodable(FormatImage format) {
        return DECODABLES.contains(format);
    }

    /**
     * Contrôle rapide, sans décoder les pixels : dimensions lisibles et nombre de pixels borné.
     *
     * @throws KupangaBusinessException 400 (illisible ou trop grande)
     */
    public static void verifierDimensions(byte[] contenu) {
        try (ImageInputStream entree = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenu))) {
            ImageReader lecteur = lecteur(entree);
            try {
                verifierDimensions(lecteur, contenu);
            } finally {
                lecteur.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw relancer(e);
        }
    }

    /**
     * Décode la photo une seule fois et la réencode en JPEG pour chaque grand côté demandé (sans agrandir).
     *
     * @param contenu photo d'un format {@linkplain #estDecodable décodable}
     * @param cotes   grands côtés voulus, ex. {@code 1600, 400}
     * @return un JPEG par côté, dans l'ordre demandé
     * @throws KupangaBusinessException 400 si la photo est illisible ou trop grande, 503 si le serveur est saturé
     */
    public static List<byte[]> versJpeg(byte[] contenu, int... cotes) {
        int coteMax = Arrays.stream(cotes).max().orElseThrow();
        int orientation = OrientationExif.lire(contenu);
        prendreJeton();
        try {
            BufferedImage image = decoder(contenu, coteMax);
            List<byte[]> resultats = new ArrayList<>(cotes.length);
            for (int cote : cotes) {
                // Orientation appliquée après la réduction : sur 1600 px au plus, pas sur l'image décodée
                resultats.add(encoderJpeg(orienter(redimensionner(image, cote), orientation)));
            }
            return resultats;
        } finally {
            DECODAGES.release();
        }
    }

    private static void prendreJeton() {
        try {
            if (DECODAGES.tryAcquire(ATTENTE_DECODAGE_SECONDES, TimeUnit.SECONDS)) return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new KupangaBusinessException("Serveur occupé, réessayez dans quelques instants",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Décode en sous-échantillonnant dès la lecture : la mémoire reste de l'ordre de {@code coteMax²} pixels,
     * même pour une photo de 100 Mpx.
     */
    static BufferedImage decoder(byte[] contenu, int coteMax) {
        try (ImageInputStream entree = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenu))) {
            ImageReader lecteur = lecteur(entree);
            try {
                int grandCote = verifierDimensions(lecteur, contenu);
                int pas = Math.max(1, grandCote / coteMax);
                ImageReadParam parametres = lecteur.getDefaultReadParam();
                parametres.setSourceSubsampling(pas, pas, 0, 0);
                BufferedImage image = lecteur.read(0, parametres);
                if (image == null) throw illisible();
                return image;
            } finally {
                lecteur.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw relancer(e);
        }
    }

    private static ImageReader lecteur(ImageInputStream entree) {
        if (entree == null) throw illisible();
        Iterator<ImageReader> lecteurs = ImageIO.getImageReaders(entree);
        if (!lecteurs.hasNext()) throw illisible();
        ImageReader lecteur = lecteurs.next();
        // Métadonnées ignorées : l'orientation est lue à part (OrientationExif), le reste n'est jamais recopié
        lecteur.setInput(entree, true, true);
        return lecteur;
    }

    /** @return le grand côté de l'image */
    private static int verifierDimensions(ImageReader lecteur, byte[] contenu) throws IOException {
        int largeur = lecteur.getWidth(0);
        int hauteur = lecteur.getHeight(0);
        if (largeur <= 0 || hauteur <= 0) throw illisible();
        long pixels = (long) largeur * hauteur;
        if (largeur > COTE_SOURCE_MAX || hauteur > COTE_SOURCE_MAX || pixels > PIXELS_MAX) {
            throw tropGrande();
        }
        // Revue R1 : au-delà de 4 Mpx, un JPEG doit être reconnu « baseline » sans aucune anomalie
        if (pixels > PIXELS_MAX_JPEG_PROGRESSIF && EnteteJpeg.estJpeg(contenu) && !EnteteJpeg.estBaseline(contenu)) {
            throw new KupangaBusinessException(
                    "Photo JPEG progressive trop grande : enregistrez-la en JPEG standard", HttpStatus.BAD_REQUEST);
        }
        return Math.max(largeur, hauteur);
    }

    private static KupangaBusinessException tropGrande() {
        return new KupangaBusinessException(
                "Photo trop grande (50 mégapixels et 12 000 pixels de côté maximum)", HttpStatus.BAD_REQUEST);
    }

    /** Applique l'orientation EXIF (1 à 8) aux pixels : le JPEG produit n'a plus de métadonnées. */
    static BufferedImage orienter(BufferedImage source, int orientation) {
        if (orientation < 2 || orientation > 8) return source;
        int l = source.getWidth();
        int h = source.getHeight();
        boolean tourne = orientation >= 5;
        int ld = tourne ? h : l;
        int hd = tourne ? l : h;
        int[] pixels = source.getRGB(0, 0, l, h, null, 0, l);
        int[] resultat = new int[pixels.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < l; x++) {
                int dx;
                int dy;
                switch (orientation) {
                    case 2 -> { dx = l - 1 - x; dy = y; }
                    case 3 -> { dx = l - 1 - x; dy = h - 1 - y; }
                    case 4 -> { dx = x; dy = h - 1 - y; }
                    case 5 -> { dx = y; dy = x; }
                    case 6 -> { dx = h - 1 - y; dy = x; }
                    case 7 -> { dx = h - 1 - y; dy = l - 1 - x; }
                    default -> { dx = y; dy = l - 1 - x; } // 8
                }
                resultat[dy * ld + dx] = pixels[y * l + x];
            }
        }
        BufferedImage oriente = new BufferedImage(ld, hd, BufferedImage.TYPE_INT_RGB);
        oriente.setRGB(0, 0, ld, hd, resultat, 0, ld);
        return oriente;
    }

    /**
     * Réduit l'image pour que son grand côté soit au plus {@code cote} (jamais d'agrandissement), par moitiés
     * successives (moins de crénelage qu'une seule réduction bilinéaire), sur fond blanc (transparence PNG/GIF).
     */
    static BufferedImage redimensionner(BufferedImage source, int cote) {
        BufferedImage courante = source;
        int grandCote = Math.max(source.getWidth(), source.getHeight());
        double ratio = Math.min(1.0, (double) cote / grandCote);
        int largeurCible = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int hauteurCible = Math.max(1, (int) Math.round(source.getHeight() * ratio));

        int largeur = source.getWidth();
        int hauteur = source.getHeight();
        // Au moins un passage : convertit aussi en RGB sur fond blanc (le JPEG n'a pas de transparence)
        do {
            largeur = Math.max(largeurCible, largeur / 2);
            hauteur = Math.max(hauteurCible, hauteur / 2);
            courante = dessiner(courante, largeur, hauteur);
        } while (largeur != largeurCible || hauteur != hauteurCible);
        return courante;
    }

    private static BufferedImage dessiner(BufferedImage source, int largeur, int hauteur) {
        BufferedImage cible = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cible.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, largeur, hauteur);
            g.drawImage(source, 0, 0, largeur, hauteur, null);
        } finally {
            g.dispose();
        }
        return cible;
    }

    /** JPEG sans aucune métadonnée (ni EXIF, ni GPS). */
    static byte[] encoderJpeg(BufferedImage image) {
        ImageWriter ecrivain = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (ImageOutputStream flux = new MemoryCacheImageOutputStream(sortie)) {
            ecrivain.setOutput(flux);
            ImageWriteParam parametres = ecrivain.getDefaultWriteParam();
            parametres.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametres.setCompressionQuality(QUALITE_JPEG);
            ecrivain.write(null, new IIOImage(image, null, null), parametres);
        } catch (IOException e) {
            throw new IllegalStateException("Encodage JPEG impossible", e);
        } finally {
            ecrivain.dispose();
        }
        return sortie.toByteArray();
    }

    private static KupangaBusinessException relancer(Exception e) {
        return e instanceof KupangaBusinessException metier ? metier : illisible();
    }

    private static KupangaBusinessException illisible() {
        return new KupangaBusinessException("Photo illisible ou endommagée", HttpStatus.BAD_REQUEST);
    }
}
