package com.kupanga.api.backoffice.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chiffres du tableau de bord du back-office, calculés en une seule lecture par {@code TableauDeBordService}.
 * Les périodes « 30 jours » se comparent aux 30 jours précédents.
 */
public record TableauDeBordDTO(
        Indicateur comptes,
        long comptesNonConfirmes,
        Indicateur biens,
        long biensArchives,
        long biensLoues,
        Indicateur premiersContacts,
        Location location,
        Quittances quittances,
        Emails emails,
        List<PointMensuel> mois,
        List<Part> parPays,
        List<Part> parVille,
        List<Part> parType,
        List<CompteRecent> derniersComptes,
        List<BienRecent> derniersBiens
) {

    /** Taux d'occupation des biens en ligne, en pourcentage entier (0 si aucun bien). */
    public int tauxOccupation() {
        return pourcentage(biensLoues, biens.valeur());
    }

    /** Nombre de points d'attention non nuls (pastille du titre « À surveiller »). */
    public long pointsAttention() {
        long n = 0;
        for (long v : new long[]{emails.echecs(), emails.enAttente(), comptesNonConfirmes, location.demandesEnAttente(),
                location.bauxEnAttente(), location.edlEnAttente(), quittances.enRetard(), location.conges()}) {
            if (v > 0) n++;
        }
        return n;
    }

    /** Plus grande valeur mensuelle, toutes séries confondues (échelle commune des graphiques). */
    public long maxMensuel() {
        long max = 0;
        for (PointMensuel p : mois) {
            max = Math.max(max, Math.max(p.inscriptions(), p.biens()));
        }
        return max;
    }

    public static int pourcentage(long part, long total) {
        return total <= 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }

    /** Valeur actuelle + nouveaux sur 30 jours, comparés aux 30 jours précédents. */
    public record Indicateur(long valeur, long nouveaux, long nouveauxPrecedents) {

        /** Écart en pourcentage avec la période précédente ; {@code null} si elle était vide. */
        public Integer variation() {
            if (nouveauxPrecedents == 0) {
                return null;
            }
            return (int) Math.round((nouveaux - nouveauxPrecedents) * 100.0 / nouveauxPrecedents);
        }
    }

    public record Location(long reservations, long actives, long conges, long terminees30j, long demandesEnAttente,
                           long bauxEnAttente, long bauxSignes30j, long edlEnAttente) {
    }

    public record Quittances(long payees, long enAttente, long enRetard) {
        public long total() {
            return payees + enAttente + enRetard;
        }

        public int partPayees() {
            return pourcentage(payees, total());
        }

        public int partEnAttente() {
            return pourcentage(enAttente, total());
        }

        public int partEnRetard() {
            return total() == 0 ? 0 : 100 - partPayees() - partEnAttente();
        }
    }

    public record Emails(long enAttente, long echecs, long envoyes24h) {
    }

    /** Un mois de l'historique : {@code cle} au format {@code AAAA-MM}, {@code libelle} court (« nov. »). */
    public record PointMensuel(String cle, String libelle, String libelleLong, long inscriptions, long biens,
                               long baux, long edl, long quittances) {
    }

    /** Part d'une répartition (pays, ville, type) ; {@code pourcentage} sur le total de la répartition. */
    public record Part(String cle, String libelle, long valeur, int pourcentage) {
    }

    public record CompteRecent(Long id, String nom, String email, boolean emailVerifie, LocalDateTime dateCreation) {
    }

    public record BienRecent(Long id, String titre, String ville, String pays, String type, String proprietaire,
                             boolean loue, LocalDateTime dateCreation) {
    }
}
