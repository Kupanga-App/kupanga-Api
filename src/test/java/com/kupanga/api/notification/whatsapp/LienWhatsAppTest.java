package com.kupanga.api.notification.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("C7-C16 : LienWhatsApp")
class LienWhatsAppTest {

    @Test
    @DisplayName("numéro E.164 : wa.me/<chiffres sans +>, texte encodé en UTF-8 (espaces en %20)")
    void numeroConnu() {
        LienWhatsAppDTO lien = LienWhatsApp.creer("+243812345678", "Bonjour Grâce, loyer : 1 200,00 $US & charges");

        assertThat(lien.numeroConnu()).isTrue();
        assertThat(lien.url()).startsWith("https://wa.me/243812345678?text=Bonjour%20Gr%C3%A2ce")
                .doesNotContain("+").doesNotContain(" ").doesNotContain("&charges").contains("%26");
        String texte = URLDecoder.decode(URI.create(lien.url()).getRawQuery().substring("text=".length()),
                StandardCharsets.UTF_8);
        assertThat(texte).isEqualTo(lien.texte());
    }

    @Test
    @DisplayName("sans numéro : wa.me/?text=… (WhatsApp fait choisir le contact)")
    void sansNumero() {
        LienWhatsAppDTO lien = LienWhatsApp.creer(null, "Bonjour");

        assertThat(lien.numeroConnu()).isFalse();
        assertThat(lien.url()).isEqualTo("https://wa.me/?text=Bonjour");
    }

    @Test
    @DisplayName("valeur non E.164 en base (jamais attendu) : ignorée, l'URL reste sur wa.me")
    void numeroInvalide_ignore() {
        LienWhatsAppDTO lien = LienWhatsApp.creer("+243@evil.example/x", "Bonjour");

        assertThat(lien.numeroConnu()).isFalse();
        assertThat(URI.create(lien.url()).getHost()).isEqualTo("wa.me");
        assertThat(lien.url()).isEqualTo("https://wa.me/?text=Bonjour");
    }
}
