package com.kupanga.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A11/W5 : la liste des origines autorisées est validée au démarrage.
 */
@DisplayName("Tests unitaires — CorsProperties")
class CorsPropertiesTest {

    @Test
    @DisplayName("Joker « * » refusé (avec les cookies, tout site pourrait récupérer un access token)")
    void joker_refuse() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CorsProperties(List.of("https://*.kupanga.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Liste absente ou vide refusée")
    void listeVide_refusee() {
        assertThatThrownBy(() -> new CorsProperties(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CorsProperties(List.of(" "))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Origine sans schéma ou avec un chemin refusée (ne correspondrait jamais à rien)")
    void formatInvalide_refuse() {
        assertThatThrownBy(() -> new CorsProperties(List.of("kupanga.lespacelibellule.com")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CorsProperties(List.of("https://kupanga.lespacelibellule.com/app")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Espaces et « / » final retirés")
    void origines_normalisees() {
        CorsProperties props = new CorsProperties(List.of(" http://localhost:4200 ", "https://kupanga.lespacelibellule.com/"));

        assertThat(props.allowedOrigins())
                .containsExactly("http://localhost:4200", "https://kupanga.lespacelibellule.com");
    }
}
