package com.kupanga.api.immobilier.validation;

import com.kupanga.api.immobilier.dto.formDTO.SignatureDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("B6 — signature : PNG en base64, structure, taille et dimensions bornées")
class SignaturePngValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valide(String signature) {
        return validator.validate(new SignatureDTO(signature)).isEmpty();
    }

    private boolean valide(byte[] png) {
        return valide(Base64.getEncoder().encodeToString(png));
    }

    // ─── Construction de PNG bloc par bloc (CRC justes) ──────────────────────

    private record Bloc(String type, byte[] donnees) {}

    private static List<Bloc> blocs(byte[] png) {
        List<Bloc> blocs = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(png);
        int position = 8;
        while (position < png.length) {
            int longueur = buffer.getInt(position);
            String type = new String(png, position + 4, 4, StandardCharsets.US_ASCII);
            blocs.add(new Bloc(type, Arrays.copyOfRange(png, position + 8, position + 8 + longueur)));
            position += 12 + longueur;
        }
        return blocs;
    }

    private static byte[] assembler(List<Bloc> blocs) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        sortie.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        for (Bloc bloc : blocs) {
            byte[] type = bloc.type().getBytes(StandardCharsets.US_ASCII);
            CRC32 crc = new CRC32();
            crc.update(type);
            crc.update(bloc.donnees());
            sortie.writeBytes(ByteBuffer.allocate(4).putInt(bloc.donnees().length).array());
            sortie.writeBytes(type);
            sortie.writeBytes(bloc.donnees());
            sortie.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
        }
        return sortie.toByteArray();
    }

    private static byte[] ihdr(int largeur, int hauteur, int profondeur) {
        return ByteBuffer.allocate(13).putInt(largeur).putInt(hauteur)
                .put((byte) profondeur).put((byte) 6).put((byte) 0).put((byte) 0).put((byte) 0).array();
    }

    private static List<Bloc> blocsDuPad() {
        return blocs(Base64.getDecoder().decode(SignaturesDeTest.signatureValide()));
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Signature du pad du front (PNG 480 × 140) acceptée, y compris reconstruite bloc par bloc")
    void signatureDuPad_acceptee() {
        assertThat(valide(SignaturesDeTest.signatureValide())).isTrue();
        assertThat(valide(assembler(blocsDuPad()))).isTrue();
    }

    @Test
    @DisplayName("Préfixe data:, caractères hors base64 ou texte quelconque → refusés")
    void pasDuBase64_refuse() {
        assertThat(valide("data:image/png;base64," + SignaturesDeTest.signatureValide())).isFalse();
        assertThat(valide("<svg onload=alert(1)>" + "A".repeat(200))).isFalse();
        assertThat(valide("A".repeat(120))).isFalse(); // base64 valide mais pas une image
    }

    @Test
    @DisplayName("Autre format d'image (JPEG) → refusé")
    void autreFormat_refuse() {
        byte[] jpeg = new byte[200];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        assertThat(valide(jpeg)).isFalse();
    }

    @Test
    @DisplayName("Dimensions énormes dans l'en-tête (bombe mémoire au rendu du PDF) → refusé")
    void dimensionsEnormes_refuse() {
        List<Bloc> blocs = blocsDuPad();
        blocs.set(0, new Bloc("IHDR", ihdr(40_000, 40_000, 8)));

        assertThat(valide(assembler(blocs))).isFalse();
        assertThat(valide(SignaturesDeTest.png(SignaturePngValidator.LARGEUR_MAX + 1, 10))).isFalse();
        assertThat(valide(SignaturesDeTest.png(10, SignaturePngValidator.HAUTEUR_MAX + 1))).isFalse();
    }

    @Test
    @DisplayName("Second IHDR géant après un premier valide (iText garde le dernier) → refusé")
    void secondIhdr_refuse() {
        List<Bloc> blocs = blocsDuPad();
        blocs.add(1, new Bloc("IHDR", ihdr(30_000, 30_000, 8)));

        assertThat(valide(assembler(blocs))).isFalse();
    }

    @Test
    @DisplayName("Bloc compressé zTXt, bloc inconnu ou iCCP de plus de 4 Ko (bombe de décompression) → refusé")
    void blocNonAutorise_refuse() {
        List<Bloc> avecZtxt = blocsDuPad();
        avecZtxt.add(1, new Bloc("zTXt", "cle\0\0xxxx".getBytes(StandardCharsets.US_ASCII)));
        List<Bloc> avecInconnu = blocsDuPad();
        avecInconnu.add(1, new Bloc("zzZZ", new byte[4]));
        List<Bloc> iccpGeant = blocsDuPad();
        iccpGeant.add(1, new Bloc("iCCP", new byte[SignaturePngValidator.ICCP_MAX_OCTETS + 1]));

        assertThat(valide(assembler(avecZtxt))).isFalse();
        assertThat(valide(assembler(avecInconnu))).isFalse();
        assertThat(valide(assembler(iccpGeant))).isFalse();
    }

    @Test
    @DisplayName("Profil de couleur iCCP court et bloc Apple iDOT (canvas Safari) → acceptés")
    void blocsSafari_acceptes() {
        List<Bloc> blocs = blocsDuPad();
        blocs.add(1, new Bloc("iCCP", new byte[SignaturePngValidator.ICCP_MAX_OCTETS]));
        blocs.add(2, new Bloc("iDOT", new byte[28]));

        assertThat(valide(assembler(blocs))).isTrue();
    }

    @Test
    @DisplayName("Données tronquées, données après IEND, CRC faux ou profondeur 16 bits → refusés")
    void structureInvalide_refuse() {
        byte[] png = assembler(blocsDuPad());

        assertThat(valide(Arrays.copyOf(png, png.length - 6))).isFalse();
        assertThat(valide(Arrays.copyOf(png, png.length + 16))).isFalse();

        byte[] crcFaux = png.clone();
        crcFaux[crcFaux.length - 1] ^= 0x01;
        assertThat(valide(crcFaux)).isFalse();

        List<Bloc> seizeBits = blocsDuPad();
        seizeBits.set(0, new Bloc("IHDR", ihdr(480, 140, 16)));
        assertThat(valide(assembler(seizeBits))).isFalse();
    }

    @Test
    @DisplayName("Plus de 200 000 caractères → refusé")
    void tropLong_refuse() {
        String signature = SignaturesDeTest.signatureValide();
        String tropLong = signature + "A".repeat(SignaturePngValidator.TAILLE_MAX_BASE64 - signature.length() + 4);

        assertThat(valide(tropLong)).isFalse();
    }
}
