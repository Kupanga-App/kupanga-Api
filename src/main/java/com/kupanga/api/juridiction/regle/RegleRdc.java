package com.kupanga.api.juridiction.regle;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.RegleJuridiction;
import org.springframework.stereotype.Component;

/**
 * Règles propres à la République démocratique du Congo (MVP Kinshasa), complétées par J3 et J4.
 * Le contenu juridique (bail, garantie, avance) doit être validé par un juriste (C9).
 */
@Component
public class RegleRdc implements RegleJuridiction {

    @Override
    public Pays pays() {
        return Pays.CD;
    }
}
