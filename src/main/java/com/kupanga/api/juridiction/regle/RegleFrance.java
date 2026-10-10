package com.kupanga.api.juridiction.regle;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.RegleJuridiction;
import org.springframework.stereotype.Component;

/** Règles propres à la France (bail loi de 1989, DPE…), complétées par J3 et J4. */
@Component
public class RegleFrance implements RegleJuridiction {

    @Override
    public Pays pays() {
        return Pays.FR;
    }
}
