package com.kupanga.api.immobilier.research.specification;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.research.dto.BienSearchDTO;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

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
                .and(parTitre(dto.titre()));
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
}