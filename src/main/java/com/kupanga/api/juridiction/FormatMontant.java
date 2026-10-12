package com.kupanga.api.juridiction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * J3 : affiche un montant dans sa devise, selon la langue du pays (« 850,00 € », « 1 200,00 $US »).
 * Utilisé par les PDF ({@code ${montants.f(contrat.loyerMensuel)}}) et les e-mails ; jamais de « € » en dur.
 *
 * @param locale langue du profil de juridiction du document
 * @param devise devise figée sur le document (ou celle du bien)
 */
public record FormatMontant(Locale locale, Devise devise) {

    /** @return le montant formaté, ou une chaîne vide si absent */
    public String f(BigDecimal montant) {
        if (montant == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(Currency.getInstance(devise.name()));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        // Espaces insécables du séparateur de milliers : rendus en espace simple dans les PDF et e-mails
        return format.format(montant).replace(' ', ' ').replace(' ', ' ');
    }

    /** Somme de deux montants, formatée (ex. loyer + charges) ; un montant absent compte pour zéro. */
    public String somme(BigDecimal a, BigDecimal b) {
        return f((a == null ? BigDecimal.ZERO : a).add(b == null ? BigDecimal.ZERO : b));
    }
}
