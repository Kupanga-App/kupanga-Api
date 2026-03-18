package com.kupanga.api.immobilier.research.specification;

import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.research.dto.BienSearchDTO;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class BienSpecification {

    /**
     * Construit une {@code Specification} JPA à partir des critères de recherche fournis.
     *
     * @param dto les critères de recherche
     * @return la spécification combinée pour filtrer les biens
     */
    public Specification<Bien> build(BienSearchDTO dto) {
        return Specification
                .where(parVilles(dto.villes()))
                .and(parPays(dto.pays()))
                .and(parCodesPostaux(dto.codesPostaux()))
                .and(parTypesBien(dto.typesBien()))
                .and(parTitre(dto.titre()))
                .and(parPois(dto.poisRequis()))
                // ─── Conditions de location ───────────────────────────────────
                .and(loyerMin(dto.loyerMin()))
                .and(loyerMax(dto.loyerMax()))
                .and(parMeuble(dto.meuble()))
                .and(parColocation(dto.colocation()))
                .and(disponibleAvant(dto.disponibleAvant()))

                // ─── Caractéristiques physiques ───────────────────────────────
                .and(surfaceMin(dto.surfaceMin()))
                .and(surfaceMax(dto.surfaceMax()))
                .and(piecesMin(dto.piecesMin()))
                .and(avecAscenseur(dto.ascenseur()))
                .and(etageMin(dto.etageMin()))
                .and(etageMax(dto.etageMax()))

                // ─── Diagnostic énergétique ───────────────────────────────────
                .and(parClassesEnergie(dto.classesEnergie()))
                .and(parModesChauffage(dto.modesChauffage()))
                .and(parClassesGes(dto.classesGes()));
    }

    /**
     * Filtre les biens par liste de villes (insensible à la casse).
     *
     * @param villes les villes à filtrer
     * @return la spécification correspondante ou null si aucun filtre
     */
    private Specification<Bien> parVilles(List<String> villes) {
        return (root, query, cb) -> {
            if (villes == null || villes.isEmpty()) return null;

            List<Expression<String>> villesLower = villes.stream()
                    .map(v -> cb.<String>literal(v.toLowerCase()))
                    .toList();

            return cb.lower(root.get("ville")).in(villesLower);
        };
    }

    /**
     * Filtre les biens par liste de pays (insensible à la casse).
     *
     * @param pays les pays à filtrer
     * @return la spécification correspondante ou null si aucun filtre
     */
    private Specification<Bien> parPays(List<String> pays) {
        return (root, query, cb) -> {
            if (pays == null || pays.isEmpty()) return null;

            List<Expression<String>> paysLower = pays.stream()
                    .map(v -> cb.<String>literal(v.toLowerCase()))
                    .toList();

            return cb.lower(root.get("pays")).in(paysLower);
        };
    }

    /**
     * Filtre les biens par liste de codes postaux.
     * Pas de lower() — les codes postaux sont insensibles à la casse par nature.
     *
     * @param codesPostaux les codes postaux à filtrer
     * @return la spécification correspondante ou null si aucun filtre
     */
    private Specification<Bien> parCodesPostaux(List<String> codesPostaux) {
        return (root, query, cb) -> {
            if (codesPostaux == null || codesPostaux.isEmpty()) return null;
            return root.get("codePostal").in(codesPostaux);
        };
    }

    /**
     * Filtre les biens par types de bien.
     *
     * @param typesBien les types de bien à filtrer
     * @return la spécification correspondante ou null si aucun filtre
     */
    private Specification<Bien> parTypesBien(List<TypeBien> typesBien) {
        return (root, query, cb) -> {
            if (typesBien == null || typesBien.isEmpty()) return null;
            return root.get("typeBien").in(typesBien);
        };
    }

    /**
     * Filtre les biens par titre (recherche partielle, insensible à la casse).
     *
     * @param titre le titre à rechercher
     * @return la spécification correspondante ou null si aucun filtre
     */
    private Specification<Bien> parTitre(String titre) {
        return (root, query, cb) -> {
            if (titre == null || titre.isBlank()) return null;
            return cb.like(
                    cb.lower(root.get("titre")),
                    "%" + titre.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filtre les biens proche des POI
     * @param poisRequis pois requis pour la recherche
     * @return la spécification correspondante ou null si aucun filtre
     */
    private Specification<Bien> parPois(List<PoiType> poisRequis) {
        return (root, query, cb) -> {
            if (poisRequis == null || poisRequis.isEmpty()) return null;

            // Subquery : le bien doit avoir un BienPoi present=true pour chaque POI demandé
            var subquery = query.subquery(Long.class);
            var bienPoi  = subquery.from(BienPoi.class);

            subquery.select(bienPoi.get("bien").get("id"))
                    .where(
                            cb.and(
                                    bienPoi.get("poiType").in(poisRequis),
                                    cb.isTrue(bienPoi.get("present"))
                            )
                    )
                    .groupBy(bienPoi.get("bien").get("id"))
                    .having(
                            cb.equal(
                                    cb.countDistinct(bienPoi.get("poiType")),
                                    (long) poisRequis.size()  // TOUS les POI demandés doivent être présents
                            )
                    );

            return root.get("id").in(subquery);
        };
    }

    // ─── Conditions de location ───────────────────────────────────────────────────

    private Specification<Bien> loyerMin(Double loyerMin) {
        return (root, query, cb) -> {
            if (loyerMin == null) return null;
            return cb.greaterThanOrEqualTo(root.get("loyerMensuel"), loyerMin);
        };
    }

    private Specification<Bien> loyerMax(Double loyerMax) {
        return (root, query, cb) -> {
            if (loyerMax == null) return null;
            return cb.lessThanOrEqualTo(root.get("loyerMensuel"), loyerMax);
        };
    }

    private Specification<Bien> parMeuble(Boolean meuble) {
        return (root, query, cb) -> {
            if (meuble == null) return null;
            return cb.equal(root.get("meuble"), meuble);
        };
    }

    private Specification<Bien> parColocation(Boolean colocation) {
        return (root, query, cb) -> {
            if (colocation == null) return null;
            return cb.equal(root.get("colocation"), colocation);
        };
    }

    private Specification<Bien> disponibleAvant(LocalDate disponibleAvant) {
        return (root, query, cb) -> {
            if (disponibleAvant == null) return null;
            // biens disponibles à partir d'une date <= la date demandée
            return cb.lessThanOrEqualTo(root.get("disponibleDe"), disponibleAvant);
        };
    }

    // ─── Caractéristiques physiques ───────────────────────────────────────────────

    private Specification<Bien> surfaceMin(Double surfaceMin) {
        return (root, query, cb) -> {
            if (surfaceMin == null) return null;
            return cb.greaterThanOrEqualTo(root.get("surfaceHabitable"), surfaceMin);
        };
    }

    private Specification<Bien> surfaceMax(Double surfaceMax) {
        return (root, query, cb) -> {
            if (surfaceMax == null) return null;
            return cb.lessThanOrEqualTo(root.get("surfaceHabitable"), surfaceMax);
        };
    }

    private Specification<Bien> piecesMin(Integer piecesMin) {
        return (root, query, cb) -> {
            if (piecesMin == null) return null;
            return cb.greaterThanOrEqualTo(root.get("nombrePieces"), piecesMin);
        };
    }

    private Specification<Bien> avecAscenseur(Boolean ascenseur) {
        return (root, query, cb) -> {
            if (ascenseur == null) return null;
            return cb.equal(root.get("ascenseur"), ascenseur);
        };
    }

    private Specification<Bien> etageMin(Integer etageMin) {
        return (root, query, cb) -> {
            if (etageMin == null) return null;
            return cb.greaterThanOrEqualTo(root.get("etage"), etageMin);
        };
    }

    private Specification<Bien> etageMax(Integer etageMax) {
        return (root, query, cb) -> {
            if (etageMax == null) return null;
            return cb.lessThanOrEqualTo(root.get("etage"), etageMax);
        };
    }

    // ─── Diagnostic énergétique ───────────────────────────────────────────────────

    private Specification<Bien> parClassesEnergie(List<ClasseEnergie> classesEnergie) {
        return (root, query, cb) -> {
            if (classesEnergie == null || classesEnergie.isEmpty()) return null;
            return root.get("classeEnergie").in(classesEnergie);
        };
    }

    // ─── Mode de chauffage ────────────────────────────────────────────────────────
    private Specification<Bien> parModesChauffage(List<ModeChauffage> modesChauffage) {
        return (root, query, cb) -> {
            if (modesChauffage == null || modesChauffage.isEmpty()) return null;
            return root.get("modeChauffage").in(modesChauffage);
        };
    }

    // ─── Classe GES ───────────────────────────────────────────────────────────────
    private Specification<Bien> parClassesGes(List<ClasseGes> classesGes) {
        return (root, query, cb) -> {
            if (classesGes == null || classesGes.isEmpty()) return null;
            return root.get("classeGes").in(classesGes);
        };
    }
}