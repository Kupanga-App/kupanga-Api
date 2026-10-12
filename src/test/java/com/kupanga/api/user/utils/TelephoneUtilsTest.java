package com.kupanga.api.user.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("C6 : TelephoneUtils")
class TelephoneUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "+243812345678,          +243812345678",
            "'+243 81 234 56 78',    +243812345678",
            "'+242 06-612.34.56',    +242066123456",
            "'0033 (6) 12 34 56 78', +33612345678",
            "' +33612345678 ',       +33612345678"
    })
    @DisplayName("normaliser : séparateurs retirés, 00 → +, format E.164")
    void normaliser_formatE164(String saisie, String attendu) {
        assertThat(TelephoneUtils.normaliser(saisie)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("normaliser : vide ou null → null (champ facultatif)")
    void normaliser_vide() {
        assertThat(TelephoneUtils.normaliser(null)).isNull();
        assertThat(TelephoneUtils.normaliser("   ")).isNull();
        assertThat(TelephoneUtils.estValide("")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0812345678",            // numéro local, indicatif inconnu
            "+0243812345678",        // indicatif commençant par 0
            "+2431234",              // trop court
            "+2438123456789012",     // plus de 15 chiffres
            "+243 81a 234 5678",     // lettre
            "+243\n812345678",       // retour à la ligne au milieu
            "+243<script>",
            "+33 06 12 34 56 78",    // 0 national après l'indicatif
            "+243 0812345678",
            "++243812345678",
            "+243 81 234 56 78 99 99 99 99 99 99 99"
    })
    @DisplayName("normaliser : saisies invalides refusées")
    void normaliser_invalide(String saisie) {
        assertThatThrownBy(() -> TelephoneUtils.normaliser(saisie)).isInstanceOf(IllegalArgumentException.class);
        assertThat(TelephoneUtils.estValide(saisie)).isFalse();
    }
}
