package com.kupanga.api.authentification.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;

/**
 * Fournit le compteur (seau Bucket4j) associé à une clé : Redis en dev/prod, mémoire en test.
 */
public interface StockageSeaux {

    Bucket seau(String cle, BucketConfiguration configuration);
}
