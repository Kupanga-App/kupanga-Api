package com.kupanga.api.email.file;

import java.time.Duration;

import static com.kupanga.api.email.file.PrioriteEmail.CRITIQUE;
import static com.kupanga.api.email.file.PrioriteEmail.NORMALE;

/**
 * M2 : nature d'un e-mail en file. La priorité décide de l'alerte en cas d'échec définitif ; la validité du lien
 * qu'il contient évite d'envoyer, après une panne, un lien déjà expiré (statut {@code PERIME}).
 */
public enum TypeEmail {

    BIENVENUE(NORMALE, null),
    /** A14 : même durée que {@code VerificationEmailServiceImpl.VALIDITE}. */
    VERIFICATION_EMAIL(CRITIQUE, Duration.ofHours(24)),
    TENTATIVE_INSCRIPTION(NORMALE, null),
    /** Même durée que le jeton de réinitialisation ({@code AuthServiceImpl.forgotPassword}). */
    RESET_MOT_DE_PASSE(CRITIQUE, Duration.ofMinutes(10)),
    MOT_DE_PASSE_MODIFIE(CRITIQUE, null),
    /** Même durée que le lien de signature ({@code ContratServiceImpl}, 72 h). */
    INVITATION_SIGNATURE_CONTRAT(CRITIQUE, Duration.ofHours(72)),
    CONTRAT_SIGNE(CRITIQUE, null),
    /** Même durée que le lien de signature ({@code EtatDesLieuxServiceImpl}, 72 h). */
    INVITATION_SIGNATURE_EDL(CRITIQUE, Duration.ofHours(72)),
    EDL_SIGNE(CRITIQUE, null),
    QUITTANCE(CRITIQUE, null);

    private final PrioriteEmail priorite;
    private final Duration validiteLien;

    TypeEmail(PrioriteEmail priorite, Duration validiteLien) {
        this.priorite = priorite;
        this.validiteLien = validiteLien;
    }

    public PrioriteEmail getPriorite() {
        return priorite;
    }

    /** @return durée de validité du lien contenu, ou {@code null} si l'e-mail ne périme pas */
    public Duration getValiditeLien() {
        return validiteLien;
    }
}
