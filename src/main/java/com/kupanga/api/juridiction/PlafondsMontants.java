package com.kupanga.api.juridiction;

import java.math.BigDecimal;

/**
 * C5 : montants maximaux acceptés dans une devise ({@code kupanga.plafonds.<DEVISE>}). Ils dépendent de la devise
 * et non du pays : 100 000 a un sens en euros ou en dollars, pas en francs congolais.
 *
 * @param loyerMensuel      loyer mensuel hors charges maximal
 * @param chargesMensuelles charges mensuelles maximales
 * @param depotGarantie     dépôt de garantie maximal
 */
public record PlafondsMontants(BigDecimal loyerMensuel, BigDecimal chargesMensuelles, BigDecimal depotGarantie) {
}
