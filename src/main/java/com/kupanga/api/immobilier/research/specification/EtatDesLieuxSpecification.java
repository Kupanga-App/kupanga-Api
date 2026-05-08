package com.kupanga.api.immobilier.research.specification;

import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.entity.TypeEtat;
import com.kupanga.api.immobilier.research.dto.EtatDesLieuxSearchDTO;
import com.kupanga.api.user.entity.Role;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class EtatDesLieuxSpecification {

    public Specification<EtatDesLieux> build(EtatDesLieuxSearchDTO dto, Long userId, Role role) {
        return Specification
                .where(parUtilisateur(userId, role))
                .and(parType(dto.type()))
                .and(parStatut(dto.statut()))
                .and(parAnnee(dto.annee()))
                .and(parMois(dto.mois()))
                .and(parBien(dto.bienId()));
    }

    private Specification<EtatDesLieux> parUtilisateur(Long userId, Role role) {
        return (root, query, cb) -> {
            if (role == Role.ROLE_PROPRIETAIRE) {
                return cb.equal(root.get("proprietaire").get("id"), userId);
            }
            return cb.equal(root.get("locataire").get("id"), userId);
        };
    }

    private Specification<EtatDesLieux> parType(TypeEtat type) {
        return (root, query, cb) -> {
            if (type == null) return null;
            return cb.equal(root.get("type"), type);
        };
    }

    private Specification<EtatDesLieux> parStatut(StatutEdl statut) {
        return (root, query, cb) -> {
            if (statut == null) return null;
            return cb.equal(root.get("statut"), statut);
        };
    }

    private Specification<EtatDesLieux> parAnnee(Integer annee) {
        return (root, query, cb) -> {
            if (annee == null) return null;
            return cb.equal(
                    cb.function("YEAR", Integer.class, root.get("dateRealisation")),
                    annee
            );
        };
    }

    private Specification<EtatDesLieux> parMois(Integer mois) {
        return (root, query, cb) -> {
            if (mois == null) return null;
            return cb.equal(
                    cb.function("MONTH", Integer.class, root.get("dateRealisation")),
                    mois
            );
        };
    }

    private Specification<EtatDesLieux> parBien(Long bienId) {
        return (root, query, cb) -> {
            if (bienId == null) return null;
            return cb.equal(root.get("bien").get("id"), bienId);
        };
    }
}
