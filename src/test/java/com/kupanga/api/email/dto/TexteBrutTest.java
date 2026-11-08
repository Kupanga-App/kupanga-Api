package com.kupanga.api.email.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.kupanga.api.email.constantes.Constante.CONTENU_MAIL_VERIFICATION_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("M1 : version texte des e-mails HTML")
class TexteBrutTest {

    @Test
    @DisplayName("Balises retirées, entités décodées, liens lisibles, en-tête et texte d'aperçu caché ignorés")
    void conversion() {
        String html = """
                <html><head><style>p{color:red}</style><title>t</title></head><body>
                <div style="display:none; max-height:0;">Aperçu caché</div>
                <p>Bonjour&nbsp;Gr&acirc;ce &amp; Paul,</p><p>Loyer : 500&nbsp;$</p>
                <a href="https://kupanga.test/signer/abc" style="x">Signer le bail</a>
                </body></html>""";

        String texte = TexteBrut.depuisHtml(html);

        assertThat(texte).contains("Bonjour Grâce & Paul,")
                .contains("Loyer : 500 $")
                .contains("Signer le bail : https://kupanga.test/signer/abc")
                .doesNotContain("<").doesNotContain("color:red").doesNotContain("Aperçu caché");
    }

    @Test
    @DisplayName("Vrai modèle d'e-mail : le lien de confirmation figure dans le texte brut")
    void vraiModele() {
        String html = String.format(CONTENU_MAIL_VERIFICATION_EMAIL, "Grâce", "https://kupanga.test/auth/verifier-email?token=T0K3N");

        String texte = TexteBrut.depuisHtml(html);

        assertThat(texte).contains("https://kupanga.test/auth/verifier-email?token=T0K3N").contains("Grâce")
                .doesNotContain("<td").doesNotContain("fonts.googleapis");
    }

    @Test
    @DisplayName("Lien sur une image seule (logo) : son texte de remplacement, pas l'adresse")
    void lienSurImage() {
        String html = """
                <a href="https://kupanga.test/"><img src="https://kupanga.test/email/logo.png" alt="kupanga">
                <!--[if !mso]><!--><img src="https://kupanga.test/email/logo-sombre.png" alt="kupanga"><!--<![endif]--></a>
                <p>Bonjour</p>
                <a href="https://kupanga.test/x"><img src="https://kupanga.test/i.png" alt=""></a>""";

        String texte = TexteBrut.depuisHtml(html);

        assertThat(texte).startsWith("kupanga\n").doesNotContain("kupanga.test/\n").endsWith("https://kupanga.test/x");
    }

    @Test
    @DisplayName("Vrai modèle d'e-mail : le texte brut commence par le nom, pas par l'adresse du logo")
    void vraiModeleLogo() {
        String html = String.format(CONTENU_MAIL_VERIFICATION_EMAIL, "Grâce", "https://kupanga.test/auth/verifier-email?token=T0K3N")
                .replace("{{URL_APP}}", "https://kupanga.test/");

        assertThat(TexteBrut.depuisHtml(html)).startsWith("kupanga");
    }

    @Test
    @DisplayName("null → texte vide")
    void nul() {
        assertThat(TexteBrut.depuisHtml(null)).isEmpty();
    }
}
