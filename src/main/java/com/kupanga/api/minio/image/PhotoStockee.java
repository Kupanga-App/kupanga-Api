package com.kupanga.api.minio.image;

/**
 * R1 : photo d'un bien stockée en deux versions publiques.
 *
 * @param url         version de {@value CompressionImage#COTE_GRANDE} px (plein écran)
 * @param urlVignette vignette de {@value CompressionImage#COTE_VIGNETTE} px (listes, cartes) ; même URL que
 *                    {@code url} pour un format non décodable stocké tel quel
 */
public record PhotoStockee(String url, String urlVignette) {
}
