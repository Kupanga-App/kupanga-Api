package com.kupanga.api.user.utils;

import java.util.Locale;

/**
 * A10 : forme canonique d'un e-mail (sans espaces autour, en minuscules).
 * Toute adresse stockée ou recherchée passe par {@link #normaliser(String)} : {@code Jean@X.com} et {@code jean@x.com}
 * désignent le même compte.
 */
public final class EmailUtils {

    private EmailUtils() {
    }

    public static String normaliser(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
