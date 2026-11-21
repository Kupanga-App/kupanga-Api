package com.kupanga.api.backoffice.service;

import com.kupanga.api.backoffice.dto.TableauDeBordDTO;
import com.kupanga.api.backoffice.dto.TableauDeBordDTO.BienRecent;
import com.kupanga.api.backoffice.dto.TableauDeBordDTO.CompteRecent;
import com.kupanga.api.backoffice.dto.TableauDeBordDTO.Emails;
import com.kupanga.api.backoffice.dto.TableauDeBordDTO.Indicateur;
import com.kupanga.api.backoffice.dto.TableauDeBordDTO.Location;
import com.kupanga.api.backoffice.dto.TableauDeBordDTO.Part;
import com.kupanga.api.backoffice.dto.TableauDeBordDTO.PointMensuel;
import com.kupanga.api.backoffice.dto.TableauDeBordDTO.Quittances;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.juridiction.Pays;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tableau de bord du back-office : indicateurs, historique sur 12 mois, répartitions et activité récente.
 * Lecture seule, en SQL agrégé (quelques requêtes au total, quel que soit le volume) plutôt qu'en chargeant
 * les entités. Les périodes relatives (30 jours, 24 heures) sont calculées par la base ({@code LOCALTIMESTAMP}),
 * comme les dates enregistrées par Hibernate.
 */
@Service
@RequiredArgsConstructor
public class TableauDeBordService {

    static final int NB_MOIS = 12;
    static final int NB_VILLES = 7;
    static final int NB_RECENTS = 6;

    private static final Locale FR = Locale.FRANCE;

    private static final String SQL_COMPTEURS = """
            SELECT
              (SELECT count(*) FROM utilisateurs WHERE NOT anonymise)                                         AS comptes,
              (SELECT count(*) FROM utilisateurs WHERE NOT anonymise
                                                   AND date_creation >= LOCALTIMESTAMP - INTERVAL '30 days')  AS comptes_30,
              (SELECT count(*) FROM utilisateurs WHERE NOT anonymise
                                                   AND date_creation >= LOCALTIMESTAMP - INTERVAL '60 days'
                                                   AND date_creation <  LOCALTIMESTAMP - INTERVAL '30 days')  AS comptes_60,
              (SELECT count(*) FROM utilisateurs WHERE NOT email_verifie AND NOT anonymise)                   AS non_confirmes,
              (SELECT count(*) FROM biens WHERE NOT archive)                                                  AS biens,
              (SELECT count(*) FROM biens WHERE created_at >= LOCALTIMESTAMP - INTERVAL '30 days')            AS biens_30,
              (SELECT count(*) FROM biens WHERE created_at >= LOCALTIMESTAMP - INTERVAL '60 days'
                                            AND created_at <  LOCALTIMESTAMP - INTERVAL '30 days')            AS biens_60,
              (SELECT count(*) FROM biens WHERE archive)                                                      AS archives,
              (SELECT count(*) FROM biens WHERE NOT archive AND locataire_id IS NOT NULL)                     AS loues,
              (SELECT count(*) FROM conversations)                                                            AS contacts,
              (SELECT count(*) FROM conversations WHERE created_at >= LOCALTIMESTAMP - INTERVAL '30 days')    AS contacts_30,
              (SELECT count(*) FROM conversations WHERE created_at >= LOCALTIMESTAMP - INTERVAL '60 days'
                                                    AND created_at <  LOCALTIMESTAMP - INTERVAL '30 days')    AS contacts_60,
              (SELECT count(*) FROM locations WHERE statut = 'RESERVEE' AND date_expiration > LOCALTIMESTAMP) AS reservations,
              (SELECT count(*) FROM locations WHERE statut = 'ACTIVE')                                        AS actives,
              (SELECT count(*) FROM locations WHERE statut = 'ACTIVE' AND date_conge IS NOT NULL)             AS conges,
              (SELECT count(*) FROM locations WHERE statut = 'TERMINEE'
                                                AND date_fin >= LOCALTIMESTAMP - INTERVAL '30 days')          AS terminees_30,
              (SELECT count(*) FROM demandes_location WHERE statut = 'EN_ATTENTE'
                                                        AND date_expiration > LOCALTIMESTAMP)                 AS demandes,
              (SELECT count(*) FROM contrats WHERE statut IN ('EN_ATTENTE_SIGNATURE_PROPRIO',
                                                              'EN_ATTENTE_SIGNATURE_LOCATAIRE'))              AS baux_attente,
              (SELECT count(*) FROM contrats WHERE statut = 'SIGNE'
                                               AND date_signature_locataire >= LOCALTIMESTAMP - INTERVAL '30 days') AS baux_30,
              (SELECT count(*) FROM etats_des_lieux WHERE statut IN ('EN_ATTENTE_SIGNATURE_PROPRIO',
                                                                     'EN_ATTENTE_SIGNATURE_LOCATAIRE'))       AS edl_attente,
              (SELECT count(*) FROM quittances WHERE statut = 'PAYEE')                                        AS q_payees,
              (SELECT count(*) FROM quittances WHERE statut = 'EN_ATTENTE')                                   AS q_attente,
              (SELECT count(*) FROM quittances WHERE statut IN ('EN_RETARD', 'IMPAYEE'))                      AS q_retard,
              (SELECT count(*) FROM emails_sortants WHERE statut IN ('A_ENVOYER', 'EN_COURS'))                AS e_attente,
              (SELECT count(*) FROM emails_sortants WHERE statut = 'ECHEC_DEFINITIF')                         AS e_echecs,
              (SELECT count(*) FROM emails_sortants WHERE statut = 'ENVOYE'
                                                      AND envoye_le >= now() - INTERVAL '24 hours')           AS e_24h
            """;

    private static final String SQL_MENSUEL = """
            SELECT 'inscriptions' AS serie, date_trunc('month', date_creation) AS mois, count(*) AS n
              FROM utilisateurs WHERE date_creation >= :debut GROUP BY 2
            UNION ALL
            SELECT 'biens', date_trunc('month', created_at), count(*)
              FROM biens WHERE created_at >= :debut GROUP BY 2
            UNION ALL
            SELECT 'baux', date_trunc('month', date_signature_locataire), count(*)
              FROM contrats WHERE statut = 'SIGNE' AND date_signature_locataire >= :debut GROUP BY 2
            UNION ALL
            SELECT 'edl', date_trunc('month', date_signature_locataire), count(*)
              FROM etats_des_lieux WHERE statut = 'SIGNE' AND date_signature_locataire >= :debut GROUP BY 2
            UNION ALL
            SELECT 'quittances', date_trunc('month', created_at), count(*)
              FROM quittances WHERE created_at >= :debut GROUP BY 2
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public TableauDeBordDTO calculer() {
        Map<String, Object> c = jdbc.queryForMap(SQL_COMPTEURS, Map.of());

        return new TableauDeBordDTO(
                new Indicateur(n(c, "comptes"), n(c, "comptes_30"), n(c, "comptes_60")),
                n(c, "non_confirmes"),
                new Indicateur(n(c, "biens"), n(c, "biens_30"), n(c, "biens_60")),
                n(c, "archives"),
                n(c, "loues"),
                new Indicateur(n(c, "contacts"), n(c, "contacts_30"), n(c, "contacts_60")),
                new Location(n(c, "reservations"), n(c, "actives"), n(c, "conges"), n(c, "terminees_30"),
                        n(c, "demandes"), n(c, "baux_attente"), n(c, "baux_30"), n(c, "edl_attente")),
                new Quittances(n(c, "q_payees"), n(c, "q_attente"), n(c, "q_retard")),
                new Emails(n(c, "e_attente"), n(c, "e_echecs"), n(c, "e_24h")),
                historique(YearMonth.now()),
                parPays(),
                parVille(),
                parType(),
                derniersComptes(),
                derniersBiens()
        );
    }

    /** 12 derniers mois, mois en cours compris ; un mois sans activité vaut 0 dans chaque série. */
    List<PointMensuel> historique(YearMonth moisCourant) {
        YearMonth premier = moisCourant.minusMonths(NB_MOIS - 1L);
        Map<String, Map<YearMonth, Long>> series = new HashMap<>();
        jdbc.query(SQL_MENSUEL,
                new MapSqlParameterSource("debut", Timestamp.valueOf(premier.atDay(1).atStartOfDay())),
                rs -> {
                    YearMonth mois = YearMonth.from(rs.getTimestamp("mois").toLocalDateTime());
                    series.computeIfAbsent(rs.getString("serie"), k -> new HashMap<>()).put(mois, rs.getLong("n"));
                });

        List<PointMensuel> points = new ArrayList<>(NB_MOIS);
        for (int i = 0; i < NB_MOIS; i++) {
            YearMonth mois = premier.plusMonths(i);
            String court = mois.getMonth().getDisplayName(TextStyle.SHORT, FR);
            String libelle = (i == 0 || mois.getMonthValue() == 1) ? court + " " + (mois.getYear() % 100) : court;
            String libelleLong = mois.getMonth().getDisplayName(TextStyle.FULL, FR) + " " + mois.getYear();
            points.add(new PointMensuel(mois.toString(), libelle, majuscule(libelleLong),
                    valeur(series, "inscriptions", mois), valeur(series, "biens", mois),
                    valeur(series, "baux", mois), valeur(series, "edl", mois), valeur(series, "quittances", mois)));
        }
        return points;
    }

    private List<Part> parPays() {
        List<Object[]> lignes = jdbc.query(
                "SELECT pays, count(*) FROM biens WHERE NOT archive GROUP BY pays ORDER BY count(*) DESC, pays",
                (rs, i) -> new Object[]{rs.getString(1), rs.getLong(2)});
        return parts(lignes, code -> libellePays(code));
    }

    /** Les {@value #NB_VILLES} premières villes, le reste regroupé dans « Autres villes ». */
    private List<Part> parVille() {
        List<Object[]> lignes = jdbc.query("""
                SELECT ville, count(*) FROM biens WHERE NOT archive AND ville IS NOT NULL
                GROUP BY ville ORDER BY count(*) DESC, ville
                """, (rs, i) -> new Object[]{rs.getString(1), rs.getLong(2)});
        if (lignes.size() > NB_VILLES + 1) {
            long autres = lignes.subList(NB_VILLES, lignes.size()).stream().mapToLong(l -> (Long) l[1]).sum();
            List<Object[]> reduites = new ArrayList<>(lignes.subList(0, NB_VILLES));
            reduites.add(new Object[]{"", autres});
            lignes = reduites;
        }
        return parts(lignes, ville -> ville.isEmpty() ? "Autres villes" : ville);
    }

    private List<Part> parType() {
        List<Object[]> lignes = jdbc.query(
                "SELECT type_bien, count(*) FROM biens WHERE NOT archive AND type_bien IS NOT NULL "
                        + "GROUP BY type_bien ORDER BY count(*) DESC, type_bien",
                (rs, i) -> new Object[]{rs.getString(1), rs.getLong(2)});
        return parts(lignes, TableauDeBordService::libelleType);
    }

    private List<CompteRecent> derniersComptes() {
        return jdbc.query("""
                SELECT id, prenom, nom, email, email_verifie, date_creation FROM utilisateurs
                WHERE NOT anonymise ORDER BY date_creation DESC, id DESC LIMIT :n
                """, new MapSqlParameterSource("n", NB_RECENTS),
                (rs, i) -> new CompteRecent(rs.getLong("id"),
                        nomComplet(rs.getString("prenom"), rs.getString("nom")),
                        rs.getString("email"), rs.getBoolean("email_verifie"),
                        rs.getTimestamp("date_creation").toLocalDateTime()));
    }

    private List<BienRecent> derniersBiens() {
        return jdbc.query("""
                SELECT b.id, b.titre, b.ville, b.pays, b.type_bien, b.locataire_id IS NOT NULL AS loue, b.created_at,
                       u.prenom, u.nom
                FROM biens b LEFT JOIN utilisateurs u ON u.id = b.proprietaire_id
                WHERE NOT b.archive ORDER BY b.created_at DESC NULLS LAST, b.id DESC LIMIT :n
                """, new MapSqlParameterSource("n", NB_RECENTS),
                (rs, i) -> {
                    Timestamp cree = rs.getTimestamp("created_at");
                    return new BienRecent(rs.getLong("id"), rs.getString("titre"), rs.getString("ville"),
                            rs.getString("pays"), libelleType(rs.getString("type_bien")),
                            nomComplet(rs.getString("prenom"), rs.getString("nom")), rs.getBoolean("loue"),
                            cree == null ? null : cree.toLocalDateTime());
                });
    }

    private static List<Part> parts(List<Object[]> lignes, java.util.function.Function<String, String> libelle) {
        long total = lignes.stream().mapToLong(l -> (Long) l[1]).sum();
        List<Part> parts = new ArrayList<>(lignes.size());
        for (Object[] l : lignes) {
            String cle = (String) l[0];
            long valeur = (Long) l[1];
            parts.add(new Part(cle, libelle.apply(cle), valeur, TableauDeBordDTO.pourcentage(valeur, total)));
        }
        return parts;
    }

    private static long valeur(Map<String, Map<YearMonth, Long>> series, String serie, YearMonth mois) {
        return series.getOrDefault(serie, Map.of()).getOrDefault(mois, 0L);
    }

    private static long n(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? 0 : ((Number) v).longValue();
    }

    static String libellePays(String code) {
        try {
            return Pays.valueOf(code).getLibelle();
        } catch (IllegalArgumentException | NullPointerException e) {
            return code;
        }
    }

    static String libelleType(String code) {
        try {
            return TypeBien.valueOf(code).getLibelle();
        } catch (IllegalArgumentException | NullPointerException e) {
            return code;
        }
    }

    private static String nomComplet(String prenom, String nom) {
        String s = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return s.isEmpty() ? "Sans nom" : s;
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(FR) + s.substring(1);
    }

}
