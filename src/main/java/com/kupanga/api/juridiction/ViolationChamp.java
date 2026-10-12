package com.kupanga.api.juridiction;

/**
 * J4 : champ d'un formulaire de bien refusé par le profil ou la règle de son pays.
 *
 * @param champ   nom de la propriété du formulaire (ex. {@code quartier})
 * @param message message destiné à l'utilisateur (sans valeur saisie)
 */
public record ViolationChamp(String champ, String message) {
}
