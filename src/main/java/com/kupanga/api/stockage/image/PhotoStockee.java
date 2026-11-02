package com.kupanga.api.stockage.image;

/**
 * R1 : photo d'un bien stockée en deux versions publiques (S2 : clés, l'URL est construite à la lecture).
 *
 * @param cle         version de {@value CompressionImage#COTE_GRANDE} px (plein écran)
 * @param cleVignette vignette de {@value CompressionImage#COTE_VIGNETTE} px (listes, cartes) ; même clé que
 *                    {@code cle} pour un format non décodable stocké tel quel
 */
public record PhotoStockee(String cle, String cleVignette) {
}
