package com.kupanga.api.email.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * La planification est désactivée en profil test : sans ce test, une durée refusée par Spring 6.1
 * (« 20s » au lieu de « PT20S ») n'apparaît qu'au démarrage réel de l'application (incident du 2026-11-02).
 */
class EnvoyeurEmailsPlanificationTest {

    private static final Pattern VALEUR_PAR_DEFAUT = Pattern.compile("\\$\\{[^:}]+:([^}]+)}");

    @Test
    @DisplayName("Durées par défaut de reprendre() lisibles par Spring 6.1 (ISO-8601 ou millisecondes)")
    void dureesParDefautValides() throws Exception {
        Scheduled planif = EnvoyeurEmails.class.getMethod("reprendre").getAnnotation(Scheduled.class);

        for (String expression : new String[]{planif.fixedDelayString(), planif.initialDelayString()}) {
            Matcher m = VALEUR_PAR_DEFAUT.matcher(expression);
            assertThat(m.matches()).as(expression).isTrue();
            String defaut = m.group(1);
            assertThatCode(() -> {
                if (defaut.startsWith("P")) {
                    Duration.parse(defaut);
                } else {
                    Long.parseLong(defaut);
                }
            }).as(defaut).doesNotThrowAnyException();
        }
    }
}
