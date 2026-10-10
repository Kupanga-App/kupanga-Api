package com.kupanga.api.juridiction.dto;

import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.ProfilJuridiction;

import java.util.Comparator;
import java.util.List;

/**
 * J6 : configuration du formulaire de bien pour un pays ({@code GET /juridictions/{pays}}). Le front construit son
 * formulaire avec ; l'API revalide toujours ({@code @ValideSelonJuridiction}). Données de configuration seulement.
 *
 * @param pays               code ISO du pays
 * @param libelle            nom du pays
 * @param devises            devises acceptées, dans l'ordre du profil
 * @param deviseDefaut       devise proposée par défaut
 * @param champsObligatoires champs du formulaire obligatoires dans ce pays (noms de {@code BienFormDTO})
 * @param champsMasques      champs sans objet dans ce pays (à ne pas afficher ni envoyer : 400 sinon)
 * @param typesBien          types de bien proposés, dans l'ordre de l'enum
 */
public record JuridictionDTO(
        Pays pays,
        String libelle,
        List<Devise> devises,
        Devise deviseDefaut,
        List<String> champsObligatoires,
        List<String> champsMasques,
        List<TypeBien> typesBien
) {

    public static JuridictionDTO de(Pays pays, ProfilJuridiction profil) {
        return new JuridictionDTO(
                pays,
                pays.getLibelle(),
                profil.devises(),
                profil.deviseDefaut(),
                profil.champsObligatoires().stream().sorted().toList(),
                profil.champsMasques().stream().sorted().toList(),
                profil.typesBien().stream().sorted(Comparator.naturalOrder()).toList());
    }

    /** Pays pris en charge (liste déroulante du formulaire). */
    public record PaysDTO(Pays pays, String libelle) {
    }
}
