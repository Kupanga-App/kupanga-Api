package com.kupanga.api.immobilier.entity;

/**
 * État d'une location (décision du 2026-11-05).
 * <pre>
 * RESERVEE ──bail signé──▶ ACTIVE ──fin──▶ TERMINEE
 *    ├─ désistement du candidat ─▶ DESISTEE
 *    ├─ retrait du propriétaire ─▶ ANNULEE
 *    └─ 14 jours sans bail signé ─▶ EXPIREE
 * </pre>
 * Une location {@code RESERVEE} dont l'échéance est passée est traitée comme {@code EXPIREE}
 * (le statut enregistré est mis à jour par la tâche planifiée ou à la prochaine action sur le bien).
 */
public enum StatutLocation {
    RESERVEE,
    ACTIVE,
    TERMINEE,
    DESISTEE,
    ANNULEE,
    EXPIREE
}
