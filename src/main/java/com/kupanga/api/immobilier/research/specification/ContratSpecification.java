package com.kupanga.api.immobilier.research.specification;

import java.math.BigDecimal;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.research.dto.ContratSearchDTO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ContratSpecification {

    public Specification<Contrat> build(ContratSearchDTO dto, Long userId) {
        return Specification
                .where(parUtilisateur(userId))
                .and(parBien(dto.bienId()))
                .and(dureeBailMin(dto.dureeBailMoisMin()))
                .and(dureeBailMax(dto.dureeBailMoisMax()))
                .and(loyerMin(dto.loyerMin()))
                .and(loyerMax(dto.loyerMax()))
                .and(dateDebutApres(dto.dateDebutApres()))
                .and(dateDebutAvant(dto.dateDebutAvant()))
                .and(parStatut(dto.statut()));
    }

    /** Lot 3 : documents dont le compte est le propriétaire ou le locataire, quel que soit son profil. */
    private Specification<Contrat> parUtilisateur(Long userId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("proprietaire").get("id"), userId),
                cb.equal(root.get("locataire").get("id"), userId));
    }

    private Specification<Contrat> parBien(Long bienId) {
        return (root, query, cb) -> {
            if (bienId == null) return null;
            return cb.equal(root.get("bien").get("id"), bienId);
        };
    }

    private Specification<Contrat> dureeBailMin(Integer min) {
        return (root, query, cb) -> {
            if (min == null) return null;
            return cb.greaterThanOrEqualTo(root.get("dureeBailMois"), min);
        };
    }

    private Specification<Contrat> dureeBailMax(Integer max) {
        return (root, query, cb) -> {
            if (max == null) return null;
            return cb.lessThanOrEqualTo(root.get("dureeBailMois"), max);
        };
    }

    private Specification<Contrat> loyerMin(BigDecimal min) {
        return (root, query, cb) -> {
            if (min == null) return null;
            return cb.greaterThanOrEqualTo(root.get("loyerMensuel"), min);
        };
    }

    private Specification<Contrat> loyerMax(BigDecimal max) {
        return (root, query, cb) -> {
            if (max == null) return null;
            return cb.lessThanOrEqualTo(root.get("loyerMensuel"), max);
        };
    }

    private Specification<Contrat> dateDebutApres(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return null;
            return cb.greaterThanOrEqualTo(root.get("dateDebut"), date);
        };
    }

    private Specification<Contrat> dateDebutAvant(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return null;
            return cb.lessThanOrEqualTo(root.get("dateDebut"), date);
        };
    }

    private Specification<Contrat> parStatut(StatutContrat statut) {
        return (root, query, cb) -> {
            if (statut == null) return null;
            return cb.equal(root.get("statut"), statut);
        };
    }
}
