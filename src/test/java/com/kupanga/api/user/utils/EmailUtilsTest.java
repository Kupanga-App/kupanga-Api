package com.kupanga.api.user.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires — EmailUtils (A10)")
class EmailUtilsTest {

    @Test
    @DisplayName("Minuscules et espaces retirés, indépendamment de la locale (pas de « ı » turc)")
    void normaliser() {
        assertThat(EmailUtils.normaliser("  Jean.DUPONT@Exemple.FR ")).isEqualTo("jean.dupont@exemple.fr");
        assertThat(EmailUtils.normaliser("INFO@KUPANGA.TEST")).isEqualTo("info@kupanga.test");
    }

    @Test
    @DisplayName("JVM en turc : « I » reste « i » (Locale.ROOT)")
    void normaliser_localeTurque() {
        Locale defaut = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(EmailUtils.normaliser("ADMIN@KUPANGA.TEST")).isEqualTo("admin@kupanga.test");
        } finally {
            Locale.setDefault(defaut);
        }
    }

    @Test
    @DisplayName("null reste null")
    void normaliser_null() {
        assertThat(EmailUtils.normaliser(null)).isNull();
    }
}
