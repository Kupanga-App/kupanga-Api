package com.kupanga.api.minio.image;

import com.kupanga.api.exception.business.KupangaBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * B5 : formats reconnus au contenu réel (signature), jamais à l'extension ni au Content-Type du client.
 */
@DisplayName("Tests unitaires — ValidationImage / FormatImage (B5)")
class ValidationImageTest {

    // ─── Échantillons : en-têtes réels des formats ───────────────────────────────

    static final byte[] JPEG = octets(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F');
    static final byte[] PNG = octets(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D);
    static final byte[] GIF = ascii("GIF89a\u0001\u0000");
    static final byte[] WEBP = concat(ascii("RIFF"), octets(0x24, 0, 0, 0), ascii("WEBPVP8 "));

    static byte[] ftyp(String marquePrincipale, String... compatibles) {
        int taille = 16 + 4 * compatibles.length;
        byte[] entete = concat(octets(0, 0, 0, taille), ascii("ftyp" + marquePrincipale), octets(0, 0, 0, 0));
        for (String marque : compatibles) entete = concat(entete, ascii(marque));
        return entete;
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> formatsAcceptes() {
        return Stream.of(
                arguments(JPEG, FormatImage.JPEG),
                arguments(PNG, FormatImage.PNG),
                arguments(GIF, FormatImage.GIF),
                arguments(WEBP, FormatImage.WEBP),
                arguments(ftyp("avif", "mif1", "miaf"), FormatImage.AVIF),
                arguments(ftyp("heic", "mif1", "heic"), FormatImage.HEIC),
                // Marque générique mif1 : le codec vient des marques compatibles
                arguments(ftyp("mif1", "mif1", "avif"), FormatImage.AVIF),
                arguments(ftyp("mif1", "mif1", "heic"), FormatImage.HEIC),
                arguments(ftyp("mif1", "mif1"), FormatImage.HEIF)
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("formatsAcceptes")
    @DisplayName("Formats des smartphones reconnus au contenu")
    void formatsAcceptes_reconnus(byte[] entete, FormatImage attendu) {
        // Nom et Content-Type du client volontairement faux : seul le contenu compte
        MultipartFile fichier = new MockMultipartFile("files", "photo.txt", "text/plain", completer(entete));

        assertThat(ValidationImage.verifier(fichier)).isEqualTo(attendu);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> contenusRefuses() {
        return Stream.of(
                arguments("HTML", ascii("<!DOCTYPE html><script>alert(1)</script>")),
                arguments("SVG", ascii("<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"/>")),
                arguments("exécutable Windows", concat(ascii("MZ"), new byte[30])),
                arguments("PDF", ascii("%PDF-1.7\n")),
                arguments("vidéo MP4", ftyp("isom", "isom", "mp42")),
                arguments("texte", ascii("dummy content"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contenusRefuses")
    @DisplayName("Contenu non image refusé en 415, même nommé .jpg avec le type image/jpeg")
    void contenusRefuses_415(String nom, byte[] contenu) {
        MultipartFile fichier = new MockMultipartFile("files", "photo.jpg", "image/jpeg", contenu);

        assertThatThrownBy(() -> ValidationImage.verifier(fichier))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    @DisplayName("Fichier vide → 400 ; plus de 10 Mo → 413")
    void videOuTropGros() {
        assertThatThrownBy(() -> ValidationImage.verifier(new MockMultipartFile("files", new byte[0])))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        byte[] tropGros = Arrays.copyOf(JPEG, (int) ValidationImage.TAILLE_MAX_OCTETS + 1);
        assertThatThrownBy(() -> ValidationImage.verifier(new MockMultipartFile("files", tropGros)))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    @DisplayName("Bien : 20 photos acceptées, 21 refusées (400)")
    void nombreMaxPhotosBien() {
        MultipartFile photo = new MockMultipartFile("files", "p.jpg", "image/jpeg", completer(JPEG));

        assertThatCode(() -> ValidationImage.verifierPhotosBien(Collections.nCopies(20, photo)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ValidationImage.verifierPhotosBien(Collections.nCopies(21, photo)))
                .isInstanceOfSatisfying(KupangaBusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Bien : une seule photo invalide refuse tout l'envoi")
    void unePhotoInvalide_refuseTout() {
        MultipartFile photo = new MockMultipartFile("files", "p.jpg", "image/jpeg", completer(JPEG));
        MultipartFile piege = new MockMultipartFile("files", "p2.jpg", "image/jpeg", ascii("<html>"));

        assertThatThrownBy(() -> ValidationImage.verifierPhotosBien(List.of(photo, piege)))
                .isInstanceOf(KupangaBusinessException.class);
    }

    // ─── Outils ───────────────────────────────────────────────────────────────────

    static byte[] octets(int... valeurs) {
        byte[] resultat = new byte[valeurs.length];
        for (int i = 0; i < valeurs.length; i++) resultat[i] = (byte) valeurs[i];
        return resultat;
    }

    static byte[] ascii(String texte) {
        return texte.getBytes(StandardCharsets.ISO_8859_1);
    }

    static byte[] concat(byte[]... parties) {
        byte[] resultat = new byte[0];
        for (byte[] partie : parties) {
            byte[] suite = Arrays.copyOf(resultat, resultat.length + partie.length);
            System.arraycopy(partie, 0, suite, resultat.length, partie.length);
            resultat = suite;
        }
        return resultat;
    }

    /** En-tête suivi de données quelconques, comme un vrai fichier. */
    static byte[] completer(byte[] entete) {
        return Arrays.copyOf(entete, entete.length + 256);
    }
}
