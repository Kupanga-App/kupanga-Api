package com.kupanga.api.email.file;

/** M2 : un e-mail {@code CRITIQUE} non distribué déclenche une alerte (log ERROR → Sentry) et passe en premier. */
public enum PrioriteEmail {
    CRITIQUE,
    NORMALE
}
