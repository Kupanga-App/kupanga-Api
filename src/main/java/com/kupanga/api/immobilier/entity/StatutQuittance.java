package com.kupanga.api.immobilier.entity;

public enum StatutQuittance {
    EN_ATTENTE,   // loyer pas encore encaissé
    PAYEE,        // loyer encaissé — quittance émise
    EN_RETARD,    // date d'échéance dépassée, non payé
    IMPAYEE       // loyer définitivement non réglé
}