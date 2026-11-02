package com.kupanga.api.stockage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

import java.net.URI;

/**
 * S1 : stockage S3 indépendant du serveur (RustFS, Garage, MinIO, S3 managé) : tout se règle ici
 * ({@code stockage.*}, variables {@code STOCKAGE_*}).
 *
 * @param endpoint         URL du serveur S3 vue par l'application (réseau interne)
 * @param urlPublique      URL du serveur S3 vue par le navigateur : photos publiques et URL présignées (P0-7)
 * @param region           région du serveur (signature des requêtes)
 * @param pathStyle        {@code true} : {@code <url>/<bucket>/<clé>} ; {@code false} : {@code <bucket>.<hôte>/<clé>}
 * @param accessKey        identifiant d'un compte de service limité aux buckets de Kupanga (jamais root)
 * @param secretKey        secret de ce compte
 * @param gererPolitiques  {@code true} : l'application pose au démarrage la lecture publique des buckets publics et
 *                         retire celle des buckets privés ; {@code false} : droits gérés par le déploiement
 *                         (serveurs sans politiques de bucket, ex. Garage)
 */
@Validated
@ConfigurationProperties(prefix = "stockage")
public record StockageProperties(
        @NotBlank String endpoint,
        @NotBlank String urlPublique,
        @NotBlank String region,
        boolean pathStyle,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        boolean gererPolitiques
) {

    public StockageProperties {
        endpoint = sansSlashFinal(endpoint);
        urlPublique = sansSlashFinal(urlPublique);
    }

    /**
     * URL publique d'un objet, sans signature (photos des buckets publics).
     * @param bucket bucket
     * @param cle    clé de l'objet
     * @return l'URL telle que le navigateur l'appellera
     */
    public String urlObjet(String bucket, String cle) {
        return prefixeBucket(bucket) + cle;
    }

    /** @return {@code <url publique>/<bucket>/} en path-style, {@code <schéma>://<bucket>.<hôte>/} sinon */
    public String prefixeBucket(String bucket) {
        if (pathStyle) {
            return urlPublique + "/" + bucket + "/";
        }
        URI uri = URI.create(urlPublique);
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        return uri.getScheme() + "://" + bucket + "." + uri.getHost() + port + "/";
    }

    /** Le secret n'apparaît jamais dans un log ou une trace. */
    @Override
    public String toString() {
        return "StockageProperties[endpoint=" + endpoint + ", urlPublique=" + urlPublique + ", region=" + region
                + ", pathStyle=" + pathStyle + ", gererPolitiques=" + gererPolitiques + "]";
    }

    private static String sansSlashFinal(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
