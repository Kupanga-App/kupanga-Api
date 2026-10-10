package com.kupanga.api.juridiction;

import java.util.List;

/**
 * Comportement propre à une juridiction (CLAUDE.md §4bis) : une implémentation par pays pris en charge
 * ({@code RegleFrance}, {@code RegleRdc}…), résolue par {@link JuridictionRegistry#regle(Pays)}.
 * Les données (devises, champs, modèles) restent dans {@link ProfilJuridiction} ; seule la logique qui ne
 * s'exprime pas en configuration vient ici (validations du bien et du contrat : J3, J4).
 */
public interface RegleJuridiction {

    /** Pays auquel s'applique cette règle (une seule implémentation par pays). */
    Pays pays();

    /**
     * J4 : contrôles d'un formulaire de bien propres au pays, qui ne s'expriment pas en configuration
     * (les champs obligatoires et masqués, types de bien, devises et plafonds sont déjà contrôlés par
     * {@link JuridictionRegistry#controlerBien}). Aucun par défaut.
     *
     * @param formulaire {@code BienFormDTO} (création) ou {@code BienUpdateDTO} (modification)
     */
    default List<ViolationChamp> validerBien(Object formulaire) {
        return List.of();
    }
}
