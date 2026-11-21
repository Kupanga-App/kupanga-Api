package com.kupanga.api.juridiction.regle;

import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.RegleJuridiction;
import com.kupanga.api.juridiction.ViolationChamp;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Règles propres à la République démocratique du Congo.
 * <p>
 * Bail d'habitation : loi n° 15/025 du 31 décembre 2015 relative aux baux à loyer non professionnels
 * (JO RDC, n° spécial du 12 janvier 2016) :
 * <ul>
 *   <li>art. 18 : la garantie locative ne peut excéder trois mois de loyer pour le bail résidentiel
 *       (art. 39 : servitude pénale et amende pour le bailleur qui perçoit davantage) ;</li>
 *   <li>art. 22 : il est interdit au bailleur d'exiger le paiement anticipatif du loyer (sanction : art. 43).</li>
 * </ul>
 * Bail professionnel (local commercial, entrepôt) : Acte uniforme OHADA portant sur le droit commercial général
 * (2010), art. 101 à 134, qui ne plafonne ni la garantie ni l'avance. Terrain nu : droit commun du louage.
 */
@Component
public class RegleRdc implements RegleJuridiction {

    /** Loi n° 15/025, art. 18, 1°. */
    public static final int GARANTIE_MAX_HABITATION = 3;

    @Override
    public Pays pays() {
        return Pays.CD;
    }

    /** À la création d'un bien : le type et les deux durées sont dans le formulaire. */
    @Override
    public List<ViolationChamp> validerBien(Object formulaire) {
        BeanWrapperImpl valeurs = new BeanWrapperImpl(formulaire);
        if (!valeurs.isReadableProperty("typeBien") || !(valeurs.getPropertyValue("typeBien") instanceof TypeBien type)) {
            // Modification sans changement de type : contrôlée par le service avec les valeurs effectives
            return List.of();
        }
        return validerConditionsBail(type, entier(valeurs, "garantieMois"), entier(valeurs, "avanceMois"));
    }

    @Override
    public List<ViolationChamp> validerConditionsBail(TypeBien type, Integer garantieMois, Integer avanceMois) {
        if (type == null || type.usageBail() != TypeBien.UsageBail.HABITATION) {
            return List.of();
        }
        List<ViolationChamp> violations = new ArrayList<>();
        if (garantieMois != null && garantieMois > GARANTIE_MAX_HABITATION) {
            violations.add(new ViolationChamp("garantieMois",
                    "En RDC, la garantie d'un logement ne peut dépasser 3 mois de loyer (loi n° 15/025, article 18)"));
        }
        if (avanceMois != null && avanceMois > 0) {
            violations.add(new ViolationChamp("avanceMois",
                    "En RDC, il est interdit d'exiger des loyers d'avance pour un logement (loi n° 15/025, article 22)"));
        }
        return violations;
    }

    private static Integer entier(BeanWrapperImpl valeurs, String champ) {
        return valeurs.isReadableProperty(champ) && valeurs.getPropertyValue(champ) instanceof Integer n ? n : null;
    }
}
