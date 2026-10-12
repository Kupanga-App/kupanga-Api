package com.kupanga.api.config;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Exécute une action une fois la transaction en cours validée (B11) : envoi WebSocket, tâche asynchrone…
 * Rien si la transaction est annulée. Sans transaction active, l'action s'exécute tout de suite.
 * L'action s'exécute hors transaction : elle ne doit pas écrire en base (rien ne serait validé).
 */
public final class ApresCommit {

    private ApresCommit() {
    }

    public static void executer(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // afterCompletion plutôt qu'afterCommit : Spring l'appelle aussi pour une synchronisation enregistrée
            // pendant un autre après-commit (afterCommit serait alors perdu sans erreur)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int statut) {
                    if (statut == STATUS_COMMITTED) {
                        action.run();
                    }
                }
            });
        } else {
            action.run();
        }
    }
}
