package com.kupanga.api.juridiction;

import com.kupanga.api.immobilier.entity.TypeBien;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Données d'une juridiction (CLAUDE.md §4bis), lues dans {@code kupanga.juridictions.<PAYS>} :
 * ce qui change d'un pays à l'autre sans changer le code.
 *
 * @param locale             langue et formats (montants, dates) des documents, ex. {@code fr-FR}
 * @param fuseau             fuseau horaire de référence, ex. {@code Europe/Paris}
 * @param devises            devises acceptées pour un bien
 * @param deviseDefaut       devise proposée par défaut (doit faire partie de {@code devises})
 * @param champsObligatoires champs du formulaire de bien obligatoires dans ce pays (noms de {@code BienFormDTO})
 * @param champsMasques      champs sans objet dans ce pays (ni affichés, ni enregistrés)
 * @param typesBien          types de bien proposés
 * @param modeleDocuments    version des modèles de bail, quittance et EDL, ex. {@code fr-v1} (figée sur chaque document)
 * @param notification       canaux de notification des parties
 */
public record ProfilJuridiction(
        Locale locale,
        ZoneId fuseau,
        List<Devise> devises,
        Devise deviseDefaut,
        Set<String> champsObligatoires,
        Set<String> champsMasques,
        Set<TypeBien> typesBien,
        String modeleDocuments,
        Notification notification
) {

    /** @param canaux canaux utilisés, dans l'ordre */
    public record Notification(List<CanalNotification> canaux) {
        public Notification {
            canaux = canaux == null ? List.of() : List.copyOf(canaux);
        }
    }

    public ProfilJuridiction {
        // La conversion par défaut fait de « fr-FR » une langue « fr-fr » : on relit l'étiquette BCP 47
        locale = locale == null ? null : Locale.forLanguageTag(locale.toString().replace('_', '-'));
        devises = devises == null ? List.of() : List.copyOf(devises);
        champsObligatoires = champsObligatoires == null ? Set.of() : Set.copyOf(champsObligatoires);
        champsMasques = champsMasques == null ? Set.of() : Set.copyOf(champsMasques);
        typesBien = typesBien == null ? Set.of() : Set.copyOf(typesBien);
        notification = notification == null ? new Notification(List.of()) : notification;
    }
}
