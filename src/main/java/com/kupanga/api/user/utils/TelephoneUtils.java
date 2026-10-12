package com.kupanga.api.user.utils;

import java.util.regex.Pattern;

/**
 * C6 : forme canonique d'un numéro de téléphone (E.164 : {@code +243812345678}).
 * <p>
 * La saisie peut contenir des espaces, points, tirets ou parenthèses ({@code +243 81 234 56 78}) et commencer
 * par {@code 00} au lieu de {@code +}. Le numéro doit être international : un numéro local ({@code 0812345678})
 * est refusé, l'indicatif du pays ne peut pas être deviné (diaspora).
 */
public final class TelephoneUtils {

    /** Saisie brute acceptée : chiffres, + en tête, séparateurs usuels. */
    private static final Pattern SAISIE = Pattern.compile("^\\+?[0-9 .()\\-]{1,30}$");

    private static final Pattern SEPARATEURS = Pattern.compile("[ .()\\-]");

    /** E.164 : + puis 8 à 15 chiffres, le premier n'est pas 0. */
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

    /**
     * 0 national laissé après l'indicatif (+33 06…, +243 081…) : le lien wa.me ne fonctionnerait pas.
     * Pas pour +242 : au Congo-Brazzaville, le 0 fait partie du numéro (+242 06 612 34 56).
     */
    private static final Pattern ZERO_NATIONAL = Pattern.compile("^\\+(33|32|243)0");

    private TelephoneUtils() {
    }

    /**
     * @return le numéro au format E.164, {@code null} si la saisie est vide
     * @throws IllegalArgumentException si la saisie n'est pas un numéro international valide
     */
    public static String normaliser(String saisie) {
        if (saisie == null || saisie.isBlank()) {
            return null;
        }
        String brut = saisie.trim();
        if (brut.length() > 30 || !SAISIE.matcher(brut).matches()) {
            throw new IllegalArgumentException("Numéro de téléphone invalide");
        }
        String numero = SEPARATEURS.matcher(brut).replaceAll("");
        if (numero.startsWith("00")) {
            numero = "+" + numero.substring(2);
        }
        if (!E164.matcher(numero).matches() || ZERO_NATIONAL.matcher(numero).find()) {
            throw new IllegalArgumentException("Numéro de téléphone invalide");
        }
        return numero;
    }

    /** Vrai si la saisie est vide ou normalisable. */
    public static boolean estValide(String saisie) {
        try {
            normaliser(saisie);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
